package lets_play.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lets_play.dto.UserResponse;
import lets_play.model.User;
import lets_play.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur REST réservé à l'administration des utilisateurs.
 * Tous les endpoints sont protégés : seul un utilisateur avec le rôle ADMIN peut y accéder.
 * La restriction ROLE_ADMIN est appliquée globalement dans SecurityConfig via :
 *   .requestMatchers("/api/users/**").hasRole("ADMIN")
 *
 * Opérations disponibles :
 *   - GET    /api/users       → liste tous les utilisateurs
 *   - GET    /api/users/{id}  → récupère un utilisateur par son id
 *   - PUT    /api/users/{id}  → modifie un utilisateur
 *   - DELETE /api/users/{id}  → supprime un utilisateur
 */
@Tag(name = "Utilisateurs", description = "Admin uniquement") // Groupe Swagger UI
@SecurityRequirement(name = "Bearer Authentication") // Affiche le cadenas 🔒 sur TOUS les endpoints de ce contrôleur
@RestController
@RequestMapping("/api/users") // Préfixe commun à tous les endpoints de ce contrôleur
@RequiredArgsConstructor      // Lombok : injection par constructeur du champ 'final' UserService
public class UserController {

    private final UserService userService; // Contient la logique métier de gestion des utilisateurs

    /**
     * Retourne la liste complète de tous les utilisateurs enregistrés.
     * Les données sont converties en UserResponse pour ne pas exposer
     * les champs sensibles (ex: mot de passe haché) dans la réponse JSON.
     */
    @Operation(summary = "Liste tous les utilisateurs")
    @GetMapping // GET /api/users
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()                        // Transforme la List<User> en flux de données
                .map(UserResponse::fromUser)     // Convertit chaque User en UserResponse (masque les champs sensibles)
                .collect(Collectors.toList());   // Collecte le résultat dans une nouvelle List<UserResponse>
        return ResponseEntity.ok(users);         // 200 OK + liste JSON
    }

    /**
     * Retourne un utilisateur spécifique par son identifiant MongoDB.
     * Lance ResourceNotFoundException (→ 404) si l'id est inconnu.
     *
     * @param id Identifiant MongoDB de l'utilisateur
     */
    @Operation(summary = "Recuperer un utilisateur par ID")
    @GetMapping("/{id}") // GET /api/users/{id}
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        // @PathVariable : lie le segment {id} de l'URL au paramètre 'id'
        // UserResponse.fromUser() : conversion User → DTO avant sérialisation JSON
        return ResponseEntity.ok(UserResponse.fromUser(userService.getUserById(id))); // 200 OK
    }

    /**
     * Met à jour les informations d'un utilisateur existant.
     * Seul un ADMIN peut modifier n'importe quel compte.
     *
     * @param id   Identifiant MongoDB de l'utilisateur à modifier
     * @param user Corps JSON contenant les nouvelles valeurs
     */
    @Operation(summary = "Modifier un utilisateur")
    @PutMapping("/{id}") // PUT /api/users/{id}
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @RequestBody User user) {  // @RequestBody : désérialise le JSON en objet User
        // Le service applique les modifications et retourne l'entité mise à jour
        // fromUser() assure que la réponse ne contient pas le mot de passe haché
        return ResponseEntity.ok(UserResponse.fromUser(userService.updateUser(id, user))); // 200 OK
    }

    /**
     * Supprime définitivement un utilisateur de la base de données.
     * Seul un ADMIN peut supprimer un compte.
     *
     * @param id Identifiant MongoDB de l'utilisateur à supprimer
     * @return 200 OK + message de confirmation
     */
    @Operation(summary = "Supprimer un utilisateur")
    @DeleteMapping("/{id}") // DELETE /api/users/{id}
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id); // Délègue la suppression au service (lance 404 si id inconnu)
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprime")); // 200 OK + confirmation JSON
    }
}