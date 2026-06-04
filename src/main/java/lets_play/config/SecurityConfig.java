package lets_play.config;

import lets_play.security.JwtFilter;
import lets_play.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration centrale de la sécurité Spring Security.
 * Définit la chaîne de filtres HTTP, les règles d'autorisation,
 * la politique de session, CORS, et les beans d'authentification.
 */
@Configuration
@EnableWebSecurity          // Active la configuration personnalisée de Spring Security (désactive les defaults)
@RequiredArgsConstructor    // Lombok : génère un constructeur injectant les champs 'final' (injection par constructeur)
public class SecurityConfig {

    private final JwtFilter jwtFilter;           // Filtre qui valide le token JWT à chaque requête
    private final RateLimitFilter rateLimitFilter; // Filtre qui limite le nombre de requêtes par client (anti-bruteforce)

    /**
     * Définit la chaîne de filtres de sécurité HTTP.
     * C'est le cœur de la configuration : chaque requête entrante passe
     * par cette chaîne dans l'ordre défini.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // --- CORS : partage de ressources entre origines différentes ---
            // Délègue la configuration CORS au bean corsConfigurationSource() défini plus bas
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // --- CSRF désactivé ---
            // Justifié ici car l'API est stateless (JWT) et ne repose pas sur des cookies de session.
            // Sur une app avec sessions et formulaires HTML, il faudrait le laisser activé.
            .csrf(csrf -> csrf.disable())

            // --- En-têtes de sécurité HTTP ---
            .headers(headers -> headers
                // X-Frame-Options: DENY → empêche l'intégration de l'app dans une <iframe> (protection clickjacking)
                .frameOptions(frame -> frame.deny())
                // X-Content-Type-Options: nosniff → empêche le navigateur de deviner le type MIME (protection MIME sniffing)
                .contentTypeOptions(content -> {})
            )

            // --- Politique de session : STATELESS ---
            // Spring Security ne crée ni ne stocke aucune session HTTP.
            // Chaque requête doit s'authentifier via son token JWT — indispensable pour une API REST.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // --- Règles d'autorisation des endpoints ---
            .authorizeHttpRequests(auth -> auth

                // Endpoints publics : inscription, connexion, refresh token, etc.
                .requestMatchers("/api/auth/**").permitAll()

                // Lecture du catalogue produits accessible sans authentification
                .requestMatchers(HttpMethod.GET, "/api/products").permitAll()

                // Documentation Swagger/OpenAPI accessible sans authentification
                // (à restreindre en production si la doc est confidentielle)
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // Gestion des utilisateurs réservée aux administrateurs (rôle ROLE_ADMIN en base)
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Toute autre requête nécessite une authentification valide
                .anyRequest().authenticated()
            )

            // --- Ajout des filtres personnalisés AVANT UsernamePasswordAuthenticationFilter ---
            // Ordre d'exécution : rateLimitFilter → jwtFilter → reste de la chaîne
            // rateLimitFilter en premier : bloque les clients dépassant le quota AVANT toute vérification JWT
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // jwtFilter ensuite : extrait et valide le token, puis alimente le SecurityContext
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build(); // Construit et retourne la chaîne de filtres configurée
    }

    /**
     * Configure les règles CORS (Cross-Origin Resource Sharing).
     * Nécessaire pour autoriser les appels depuis un frontend tournant
     * sur un port ou domaine différent (ex: React :3000, Angular :4200, Vite :5173).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées : uniquement les frontends de développement local
        // ⚠️ En production, remplacer par le(s) domaine(s) réel(s) de l'application
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",  // React (Create React App)
            "http://localhost:4200",  // Angular CLI
            "http://localhost:5173"   // Vite / Vue / React
        ));

        // Méthodes HTTP autorisées dans les requêtes cross-origin
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // OPTIONS est indispensable pour les requêtes "preflight" envoyées par le navigateur

        // En-têtes autorisés dans les requêtes cross-origin
        // Authorization : pour le token JWT — Content-Type : pour le body JSON
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Autorise l'envoi de credentials (cookies, en-têtes Authorization) dans les requêtes cross-origin
        config.setAllowCredentials(true);

        // Durée de mise en cache du résultat preflight par le navigateur (en secondes) = 1 heure
        // Réduit le nombre de requêtes OPTIONS répétitives
        config.setMaxAge(3600L);

        // Applique cette configuration CORS à tous les chemins de l'API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Bean d'encodage des mots de passe avec BCrypt.
     * BCrypt intègre un sel aléatoire et un facteur de coût (par défaut : 10 rounds),
     * ce qui le rend résistant aux attaques par dictionnaire et rainbow tables.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose l'AuthenticationManager comme bean Spring injectable.
     * Utilisé typiquement dans AuthService pour déclencher l'authentification
     * lors du login (appel à authenticationManager.authenticate(...)).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // Récupère le manager configuré automatiquement par Spring Boot
    }
}