package lets_play.dto;

import lets_play.model.User;
import lombok.Data;

/**
 * DTO (Data Transfer Object) représentant la réponse renvoyée au client
 * pour les opérations sur les utilisateurs.
 *
 * Rôle principal : filtrer les données de l'entité User avant sérialisation JSON,
 * en exposant uniquement les champs non sensibles.
 * Le champ 'password' (hash BCrypt) est volontairement absent — il ne doit
 * jamais transiter dans une réponse HTTP.
 */
@Data // Lombok : génère automatiquement getters, setters, equals(), hashCode() et toString()
public class UserResponse {

    private String id;    // Identifiant MongoDB de l'utilisateur (ex: "664a1f...")
    private String name;  // Nom affiché de l'utilisateur
    private String email; // Adresse email (identifiant de connexion)
    private String role;  // Rôle de l'utilisateur : "USER" ou "ADMIN"
    // ⚠️ 'password' absent intentionnellement : le hash BCrypt ne doit jamais être exposé

    /**
     * Méthode de conversion statique : transforme une entité User en UserResponse.
     * Appelée dans UserController et AuthController pour toutes les réponses
     * impliquant des données utilisateur.
     *
     * Exemple d'utilisation :
     *   UserResponse dto = UserResponse.fromUser(user);
     *
     * @param user Entité User récupérée depuis la base MongoDB
     * @return UserResponse prêt à être sérialisé en JSON
     */
    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse(); // Création de l'objet DTO vide
        response.setId(user.getId());         // Copie de l'id MongoDB
        response.setName(user.getName());     // Copie du nom
        response.setEmail(user.getEmail());   // Copie de l'email
        response.setRole(user.getRole());     // Copie du rôle
        // user.getPassword() n'est pas copié → le hash reste en base, invisible côté client
        return response; // Retourne le DTO peuplé, prêt pour la sérialisation JSON
    }
}