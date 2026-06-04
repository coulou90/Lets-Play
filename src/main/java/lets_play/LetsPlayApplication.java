package lets_play;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée principal de l'application Spring Boot "Let's Play".
 * C'est ici que la JVM démarre et que le contexte Spring est initialisé.
 *
 * Au démarrage, Spring Boot va automatiquement :
 *   - Scanner tous les beans dans le package 'lets_play' et ses sous-packages
 *   - Configurer la connexion MongoDB via application.properties
 *   - Démarrer le serveur HTTP embarqué (Tomcat, port 8080 par défaut)
 *   - Appliquer la configuration de sécurité (SecurityConfig)
 */
@SpringBootApplication // Annotation composite qui regroupe 3 annotations :
                       //   @Configuration          → cette classe peut déclarer des beans
                       //   @EnableAutoConfiguration → active la configuration automatique Spring Boot
                       //   @ComponentScan           → scanne 'lets_play' et tous ses sous-packages
                       //                              (@Component, @Service, @Repository, @Controller...)
public class LetsPlayApplication {

    /**
     * Méthode main — point d'entrée JVM standard.
     * SpringApplication.run() démarre le contexte Spring complet :
     *   1. Charge application.properties (MongoDB URI, JWT secret, port...)
     *   2. Initialise et injecte tous les beans
     *   3. Démarre le serveur Tomcat embarqué
     *
     * @param args Arguments de ligne de commande (ex: --server.port=9090)
     */
    public static void main(String[] args) {
        SpringApplication.run(LetsPlayApplication.class, args); // Lance toute l'application en une ligne
    }
}