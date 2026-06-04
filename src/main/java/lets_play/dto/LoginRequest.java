package lets_play.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO (Data Transfer Object) représentant le corps de la requête de connexion.
 * Utilisé exclusivement pour recevoir et valider les données envoyées
 * sur POST /api/auth/login — ne correspond à aucune entité en base.
 *
 * La validation est déclenchée par @Valid dans AuthController.
 * En cas d'échec, Spring retourne automatiquement un 400 Bad Request.
 */
@Data // Lombok : génère automatiquement getters, setters, equals(), hashCode() et toString()
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire") // Refuse null, "" et "   " (chaîne vide ou espaces)
    @Email(message = "Format email invalide")       // Vérifie la structure email : doit contenir @ et un domaine valide
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire") // Refuse null, "" et les chaînes vides/espaces
    // ⚠️ Pas de contrainte de longueur ou de complexité ici : la validation métier du mot de passe
    // n'a pas de sens au login (on vérifie juste que le champ n'est pas vide avant de comparer avec le hash BCrypt)
    private String password;
}