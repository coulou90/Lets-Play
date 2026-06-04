package lets_play.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre de sécurité JWT — intercepte chaque requête HTTP pour valider
 * le token Bearer et alimenter le SecurityContext de Spring Security.
 *
 * Flux d'exécution pour chaque requête :
 *   1. Lire le header "Authorization"
 *   2. Extraire et valider le token JWT
 *   3. Charger l'utilisateur depuis la base
 *   4. Injecter l'authentification dans le SecurityContext
 *   5. Passer la requête au filtre suivant
 */
@Component        // Déclare ce filtre comme bean Spring, détecté automatiquement par SecurityConfig
@RequiredArgsConstructor // Lombok : injection par constructeur des champs 'final'
public class JwtFilter extends OncePerRequestFilter {
    // OncePerRequestFilter garantit que ce filtre s'exécute exactement UNE SEULE FOIS
    // par requête HTTP, même en cas de forward ou redirect internes

    // Logger SLF4J pour tracer les tentatives avec tokens invalides
    // Utile pour détecter des tentatives d'intrusion ou des bugs côté client
    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;                     // Validation et extraction des claims du JWT
    private final UserDetailsService userDetailsService; // Chargement de l'utilisateur depuis MongoDB

    /**
     * Méthode principale du filtre — exécutée à chaque requête entrante.
     *
     * @param request     Requête HTTP entrante
     * @param response    Réponse HTTP sortante
     * @param filterChain Chaîne de filtres — appeler doFilter() pour passer au suivant
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Étape 1 — Lecture du header Authorization
        String authHeader = request.getHeader("Authorization");

        // Si le header est absent ou ne commence pas par "Bearer ",
        // la requête est publique ou mal formée → on laisse passer sans authentifier.
        // Les règles de SecurityConfig décideront si la route est accessible ou non.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Passe au filtre suivant sans authentifier
            return; // Stop — ne pas continuer l'exécution de ce filtre
        }

        // Étape 2 — Extraction du token brut (supprime le préfixe "Bearer ")
        String token = authHeader.substring(7);

        // Étape 3 — Validation du token (signature, expiration, format)
        if (!jwtUtil.isTokenValid(token)) {
            // Token invalide : log de l'IP pour détecter des attaques par force brute ou rejeu
            // ⚠️ On ne retourne PAS de 401 ici — on laisse Spring Security gérer le refus
            // en passant au filtre suivant sans alimenter le SecurityContext
            log.warn("Token JWT invalide depuis IP: {}", request.getRemoteAddr());
            filterChain.doFilter(request, response); // Passe sans authentifier → Spring Security refusera l'accès
            return; // Stop
        }

        // Étape 4 — Extraction de l'email depuis les claims du token validé
        String email = jwtUtil.extractEmail(token);

        // Double vérification avant d'alimenter le SecurityContext :
        // - email != null          → le claim email est présent dans le token
        // - getAuthentication() == null → l'utilisateur n'est pas déjà authentifié dans ce contexte
        //   (évite d'écraser une authentification existante sur la même requête)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Chargement de l'utilisateur depuis MongoDB via email
            // Récupère aussi les rôles/autorités (ROLE_USER, ROLE_ADMIN) nécessaires pour les autorisations
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Construction du token d'authentification Spring Security
            // Paramètres : principal (UserDetails), credentials (null car JWT), authorities (rôles)
            // credentials = null : le mot de passe n'est pas nécessaire après validation du JWT
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,                  // L'utilisateur authentifié
                            null,                         // Credentials (inutiles après validation JWT)
                            userDetails.getAuthorities()); // Rôles → utilisés par hasRole() dans SecurityConfig

            // Enrichit le token d'authentification avec les détails de la requête HTTP
            // (adresse IP, session ID) — utile pour les logs d'audit et la traçabilité
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Injection de l'authentification dans le SecurityContext
            // À partir de ce moment, Spring Security considère l'utilisateur comme authentifié
            // pour toute la durée du traitement de cette requête
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Étape 5 — Passage au filtre suivant dans la chaîne (puis au contrôleur)
        // Appelé dans TOUS les cas (token valide ou non) — c'est Spring Security
        // qui bloquera la requête si l'endpoint exige une authentification
        filterChain.doFilter(request, response);
    }
}