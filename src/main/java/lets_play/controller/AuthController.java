package lets_play.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lets_play.dto.LoginRequest;
import lets_play.dto.RegisterRequest;
import lets_play.model.User;
import lets_play.repository.UserRepository;
import lets_play.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur REST gérant l'authentification des utilisateurs.
 * Expose deux endpoints publics (non protégés par JWT) :
 *   - POST /api/auth/register : création de compte
 *   - POST /api/auth/login    : connexion et obtention du token JWT
 */
@Tag(name = "Authentification", description = "Register et Login") // Groupe ces endpoints sous l'onglet "Authentification" dans Swagger UI
@RestController     // Combines @Controller + @ResponseBody : chaque méthode retourne directement du JSON
@RequestMapping("/api/auth")    // Préfixe commun à tous les endpoints de ce contrôleur
@RequiredArgsConstructor        // Lombok : injection par constructeur des champs 'final'
public class AuthController {

    private final UserRepository userRepository;  // Accès aux données utilisateurs en base (MongoDB)
    private final PasswordEncoder passwordEncoder; // BCrypt : hachage et vérification des mots de passe
    private final JwtUtil jwtUtil;                 // Utilitaire de génération et validation des tokens JWT

    /**
     * Endpoint d'inscription : crée un nouveau compte utilisateur.
     * Accessible sans authentification (configuré dans SecurityConfig).
     *
     * @param request DTO contenant name, email, password (validés via @Valid)
     * @return 201 Created avec message de succès, ou 409 Conflict si l'email existe déjà
     */
    @Operation(summary = "Creer un compte", description = "Enregistre un nouvel utilisateur") // Documentation Swagger
    @PostMapping("/register") // POST /api/auth/register
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // @Valid : déclenche la validation des contraintes définies dans RegisterRequest (ex: @NotBlank, @Email)
        // @RequestBody : désérialise le JSON du corps de la requête en objet RegisterRequest

        // Vérifie si l'email est déjà utilisé pour éviter les doublons en base
        // ⚠️ Message volontairement vague ("erreur lors de l'inscription") pour ne pas confirmer
        // à un attaquant qu'un compte existe avec cet email (énumération de comptes)
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Une erreur est survenue lors de l'inscription"));
        }

        // Construction de l'entité User à partir du DTO
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Le mot de passe est haché (jamais stocké en clair)
        user.setRole("USER"); // Rôle par défaut : tous les nouveaux inscrits sont de simples utilisateurs

        userRepository.save(user); // Persistance en base MongoDB

        // 201 Created : la ressource (compte) a bien été créée
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Compte cree avec succes"));
    }

    /**
     * Endpoint de connexion : vérifie les credentials et retourne un token JWT.
     * Accessible sans authentification (configuré dans SecurityConfig).
     *
     * @param request DTO contenant email et password
     * @return 200 OK avec le token JWT + rôle + nom, ou 401 Unauthorized si credentials invalides
     */
    @Operation(summary = "Se connecter", description = "Retourne un token JWT") // Documentation Swagger
    @PostMapping("/login") // POST /api/auth/login
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        // Recherche de l'utilisateur par email — retourne null si inexistant
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Double vérification en une seule condition :
        // 1. user == null         → email inconnu en base
        // 2. !passwordEncoder.matches(...) → mot de passe incorrect
        // ⚠️ Le message est identique dans les deux cas : "Email ou mot de passe incorrect"
        // Bonne pratique de sécurité : ne pas indiquer lequel des deux est faux (évite l'énumération de comptes)
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou mot de passe incorrect"));
        }

        // Génération du token JWT signé contenant l'email et le rôle de l'utilisateur
        // Ce token sera ensuite envoyé par le client dans le header "Authorization: Bearer <token>"
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // 200 OK : retourne le token + informations utiles au frontend pour personnaliser l'interface
        return ResponseEntity.ok(Map.of(
                "token", token,        // Token JWT à stocker côté client (localStorage ou mémoire)
                "role", user.getRole(), // Permet au frontend d'adapter l'UI selon les droits (ex: menu admin)
                "name", user.getName()  // Permet d'afficher le prénom de l'utilisateur connecté
        ));
    }
}