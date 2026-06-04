package lets_play.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la documentation OpenAPI (Swagger UI) pour l'API Let's Play.
 * Cette classe personnalise les métadonnées affichées sur la page Swagger
 * et configure l'authentification JWT pour les endpoints sécurisés.
 */
@Configuration // Indique à Spring que cette classe contient des beans à enregistrer dans le contexte applicatif
public class OpenApiConfig {

    /**
     * Crée et configure le bean OpenAPI principal.
     * Ce bean est automatiquement détecté par springdoc-openapi pour générer
     * la documentation accessible via /swagger-ui.html.
     *
     * @return une instance OpenAPI personnalisée
     */
    @Bean // Déclare que la méthode retourne un bean géré par Spring
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // --- Informations générales de l'API ---
                .info(new Info()
                        .title("Let's Play API")               // Titre affiché en haut de Swagger UI
                        .version("1.0.0")                      // Version de l'API
                        .description("API REST sécurisée pour la gestion des utilisateurs et produits") // Description courte
                        .contact(new Contact()
                                .name("coulou90")              // Nom du mainteneur / auteur
                                .url("https://github.com/coulou90/Lets-Play"))) // Lien vers le dépôt du projet

                // --- Sécurité globale ---
                // Applique le schéma "Bearer Authentication" à tous les endpoints par défaut.
                // Les endpoints peuvent individuellement surcharger ou ignorer cette exigence.
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))

                // --- Définition des composants réutilisables ---
                .components(new Components()
                        // Enregistre le schéma de sécurité sous le nom "Bearer Authentication"
                        // Ce nom doit correspondre exactement à celui utilisé dans addSecurityItem() ci-dessus
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP) // Type HTTP (par opposition à apiKey ou oauth2)
                                        .scheme("bearer")               // Schéma HTTP standard pour les tokens Bearer
                                        .bearerFormat("JWT")            // Précision optionnelle : le token est au format JWT
                                        .description("Entre ton token JWT ici"))); // Texte d'aide affiché dans le champ Authorize de Swagger UI
    }
}