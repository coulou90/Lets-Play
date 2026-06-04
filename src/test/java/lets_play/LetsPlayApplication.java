package lets_play;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Point d'entrée principal de l'application Spring Boot "Let's Play".
 * C'est ici que la JVM démarre et que le contexte Spring est initialisé.
 *
 * Au démarrage, Spring Boot va automatiquement :
 *   - Scanner et enregistrer tous les beans (@Component, @Service, @Repository...)
 *   - Configurer la connexion MongoDB
 *   - Démarrer le serveur HTTP embarqué (Tomcat par défaut)
 *   - Appliquer la configuration de sécurité (SecurityConfig)
 */
@SpringBootApplication // Annotation composite qui regroupe 3 annotations :
                       //   @Configuration     → cette classe peut déclarer des beans
                       //   @EnableAutoConfiguration → active la configuration automatique Spring Boot
                       //   @ComponentScan     → scanne le package courant et ses sous-packages
                       //                        pour détecter les beans (@Component, @Service, etc.)

@EnableMongoRepositories(basePackages = "lets_play.repository")
// Active la détection automatique des interfaces Repository MongoDB
// dans le package spécifié (UserRepository, ProductRepository...)
// Spring génère automatiquement les implémentations à la compilation
// ⚠️ Sans cette annotation, les @Autowired sur les repositories échoueraient au démarrage
public class LetsPlayApplication {

    /**
     * Méthode main — point d'entrée JVM standard.
     * SpringApplication.run() démarre le contexte Spring complet :
     *   1. Charge application.properties
     *   2. Initialise tous les beans
     *   3. Démarre le serveur Tomcat embarqué (port 8080 par défaut)
     *
     * @param args Arguments de ligne de commande (ex: --server.port=9090)
     */
    public static void main(String[] args) {
        SpringApplication.run(LetsPlayApplication.class, args); // Lance toute l'application en une ligne
    }
}