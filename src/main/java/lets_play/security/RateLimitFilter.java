package lets_play.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre de limitation du nombre de requêtes (Rate Limiting) par adresse IP.
 * Protège l'API contre deux types d'abus :
 *   - Attaques par force brute sur /api/auth/login (quota strict : 5 req/min)
 *   - Surcharge générale de l'API (quota souple : 20 req/min)
 *
 * Basé sur l'algorithme "Token Bucket" via la librairie Bucket4j :
 * chaque IP dispose d'un seau de jetons rechargé à intervalle régulier.
 * Chaque requête consomme 1 jeton — si le seau est vide, la requête est bloquée.
 *
 * Exécuté EN PREMIER dans la chaîne de filtres (avant JwtFilter)
 * pour bloquer les floods AVANT tout traitement applicatif coûteux.
 */
@Component // Bean Spring détecté automatiquement et enregistré dans SecurityConfig
public class RateLimitFilter extends OncePerRequestFilter {
    // OncePerRequestFilter : garantit une seule exécution par requête HTTP

    // Table des buckets pour les routes API générales — une entrée par IP
    // ConcurrentHashMap : thread-safe pour les accès concurrents multi-utilisateurs
    private final ConcurrentHashMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    // Table des buckets dédiés au login — quota plus strict pour limiter le bruteforce
    // Séparé de apiBuckets pour appliquer des règles différentes sur /api/auth/login
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    /**
     * Crée un bucket pour les routes API générales.
     * Quota : 20 requêtes par minute par IP.
     * Refill.greedy : recharge les jetons dès qu'ils sont disponibles
     * (pas d'attente de la fin de la minute complète).
     *
     * @return Nouveau bucket configuré à 20 req/min
     */
    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(
            20,                                  // Capacité maximale du seau : 20 jetons
            Refill.greedy(20, Duration.ofMinutes(1)) // Recharge : 20 jetons toutes les 60 secondes
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Crée un bucket strict pour l'endpoint de login.
     * Quota : 5 tentatives par minute par IP.
     * Limite volontairement basse pour ralentir les attaques par dictionnaire
     * ou force brute sur les mots de passe.
     *
     * @return Nouveau bucket configuré à 5 req/min
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(
            5,                                  // Capacité maximale : 5 jetons seulement
            Refill.greedy(5, Duration.ofMinutes(1)) // Recharge : 5 jetons toutes les 60 secondes
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Méthode principale du filtre — exécutée à chaque requête entrante.
     * Détermine le bucket approprié selon l'IP et le chemin,
     * puis autorise ou bloque la requête.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip   = request.getRemoteAddr(); // Adresse IP du client — clé d'identification du bucket
        String path = request.getRequestURI(); // Chemin de la requête (ex: "/api/auth/login")

        // Exclusion du rate limiting pour les ressources Swagger/OpenAPI
        // Ces ressources sont chargées automatiquement par le navigateur au démarrage
        // de Swagger UI — les limiter provoquerait des erreurs d'affichage de la doc
        if (path.contains("/swagger-ui") ||
            path.contains("/api-docs")   ||
            path.contains("/swagger-resources")) {
            filterChain.doFilter(request, response); // Passe directement sans consommer de jeton
            return;
        }

        // Détecte si la requête cible l'endpoint de login pour appliquer le quota strict
        boolean isLoginEndpoint = path.contains("/api/auth/login");

        // Récupère le bucket existant pour cette IP, ou en crée un nouveau si première visite
        // computeIfAbsent : opération atomique thread-safe — pas de double création possible
        Bucket bucket = isLoginEndpoint
                ? authBuckets.computeIfAbsent(ip, k -> createAuthBucket()) // 5 req/min pour le login
                : apiBuckets.computeIfAbsent(ip, k -> createApiBucket());  // 20 req/min pour le reste

        // Tente de consommer 1 jeton dans le seau de l'IP
        if (bucket.tryConsume(1)) {
            // Jeton disponible → requête autorisée, passe au filtre suivant (JwtFilter)
            filterChain.doFilter(request, response);
        } else {
            // Seau vide → quota dépassé, requête bloquée immédiatement
            // La réponse est construite manuellement ici car on est AVANT Spring MVC
            // (GlobalExceptionHandler n'est pas encore accessible à ce stade)
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // HTTP 429
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Message contextuel selon le type de limite atteinte
            String message = isLoginEndpoint
                    // Message spécifique login : indique implicitement une protection anti-bruteforce
                    ? "{\"status\":429,\"message\":\"Trop de tentatives de connexion. Reessayez dans une minute.\"}"
                    // Message générique API : ne révèle pas la limite exacte
                    : "{\"status\":429,\"message\":\"Trop de requetes. Reessayez dans une minute.\"}";

            response.getWriter().write(message); // Écriture directe dans le flux de réponse HTTP
        }
    }
}