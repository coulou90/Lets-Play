package lets_play.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO (Data Transfer Object) représentant le corps de la requête d'inscription.
 * Utilisé exclusivement pour recevoir et valider les données envoyées
 * sur POST /api/auth/register — ne correspond à aucune entité en base.
 *
 * La validation est déclenchée par @Valid dans AuthController.
 * En cas d'échec, Spring retourne automatiquement un 400 Bad Request
 * avec les messages d'erreur définis sur chaque contrainte.
 */
@Data // Lombok : génère automatiquement getters, setters, equals(), hashCode() et toString()
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire") // Refuse null, "" et les chaînes composées uniquement d'espaces
    private String name;

    @NotBlank(message = "L'email est obligatoire") // Vérifie que le champ est présent et non vide
    @Email(message = "Format email invalide")       // Vérifie la structure : doit contenir @ et un domaine valide
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire") // Évalué EN PREMIER : bloque null/"" avant que @Pattern soit testé
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
        // (?=.*[A-Z])        → au moins une lettre majuscule
        // (?=.*[0-9])        → au moins un chiffre
        // (?=.*[!@#$%^&*])   → au moins un caractère spécial parmi : ! @ # $ % ^ & *
        // .{8,}              → longueur minimale de 8 caractères au total
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial"
    )
    private String password;
}