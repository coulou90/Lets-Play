package lets_play.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Entité représentant un utilisateur dans la base de données MongoDB.
 * Chaque instance correspond à un document dans la collection "users".
 *
 * Structure du document MongoDB :
 * {
 *   "_id"      : "664a1f...",   (ObjectId généré par MongoDB)
 *   "name"     : "John Doe",
 *   "email"    : "john@example.com",
 *   "password" : "$2a$10$...",  (hash BCrypt — jamais visible en JSON)
 *   "role"     : "USER"
 * }
 */
@Data            // Lombok : génère getters, setters, equals(), hashCode(), toString()
@NoArgsConstructor  // Lombok : génère un constructeur vide — requis par MongoDB pour désérialiser les documents
@AllArgsConstructor // Lombok : génère un constructeur avec tous les champs — utile pour les tests
@Document(collection = "users") // Indique à Spring Data que cette classe est mappée
                                 // sur la collection MongoDB "users"
                                 // Sans cette annotation, Spring Data ignorerait la classe
public class User {

    @Id // Marque ce champ comme identifiant primaire MongoDB
        // Spring Data le mappe automatiquement sur le champ "_id" du document
        // La valeur est un ObjectId MongoDB sérialisé en String (ex: "664a1f3b...")
    private String id;

    private String name; // Nom affiché de l'utilisateur — aucune contrainte d'unicité

    @Indexed(unique = true) // Crée un index unique sur le champ "email" dans MongoDB
                            // Garantit l'unicité au niveau base (en complément du check dans AuthController)
                            // Améliore aussi les performances de findByEmail() et existsByEmail()
    private String email;

    @JsonIgnore // Indique à Jackson de ne JAMAIS inclure ce champ dans la sérialisation JSON
                // Le hash BCrypt ne transitera donc jamais dans une réponse HTTP
                // même si un endpoint retourne directement un objet User par erreur
                // ⚠️ Défense en profondeur : UserResponse offre une protection similaire
                //    mais @JsonIgnore protège même sans passer par le DTO
    private String password;

    private String role; // Rôle de l'utilisateur : "USER" ou "ADMIN"
                         // Préfixé en "ROLE_" dans UserDetailsServiceImpl pour Spring Security
                         // Ex : "ADMIN" → "ROLE_ADMIN" → reconnu par hasRole("ADMIN")
}