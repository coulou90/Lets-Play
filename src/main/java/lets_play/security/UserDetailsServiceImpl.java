package lets_play.security;

import lets_play.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implémentation du contrat UserDetailsService de Spring Security.
 * Fait le lien entre la base de données MongoDB et le système
 * d'authentification de Spring Security.
 *
 * Utilisée par JwtFilter à chaque requête authentifiée pour :
 *   1. Charger l'utilisateur depuis MongoDB via son email
 *   2. Construire un objet UserDetails avec ses rôles
 *   3. Alimenter le SecurityContext pour les vérifications d'autorisation
 */
@Service             // Bean Spring géré par le conteneur IoC
@RequiredArgsConstructor // Lombok : injection par constructeur du champ 'final'
public class UserDetailsServiceImpl implements UserDetailsService {
    // UserDetailsService : interface Spring Security avec une seule méthode à implémenter
    // Spring l'utilise automatiquement pour l'authentification dès qu'un bean est détecté

    private final UserRepository userRepository; // Accès MongoDB pour retrouver l'utilisateur par email

    /**
     * Charge un utilisateur depuis la base par son email (= "username" au sens Spring Security).
     * Appelée automatiquement par JwtFilter après validation du token JWT.
     *
     * @param email Email de l'utilisateur extrait du token JWT
     * @return UserDetails contenant email, hash du mot de passe et rôles
     * @throws UsernameNotFoundException si aucun utilisateur ne correspond à cet email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Recherche en base — le paramètre s'appelle "username" par convention Spring Security
        // mais dans cette application l'identifiant est l'email, pas un nom d'utilisateur
        lets_play.model.User user = userRepository.findByEmail(email)
                // Si l'email est inconnu : exception Spring Security → interceptée par JwtFilter
                // ⚠️ Le message inclut l'email — acceptable dans les logs serveur,
                // mais ne doit jamais remonter dans la réponse HTTP au client
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Construction du UserDetails Spring Security à partir de l'entité MongoDB
        // Utilise l'implémentation standard User de Spring Security (import à ne pas confondre
        // avec lets_play.model.User — d'où le nom qualifié complet plus haut)
        return new User(
                user.getEmail(),    // Principal : identifiant de l'utilisateur dans le SecurityContext
                user.getPassword(), // Hash BCrypt : utilisé par Spring Security pour la vérification
                                    // lors de l'authentification classique (non utilisé en JWT)
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                // Conversion du rôle métier ("USER" ou "ADMIN") en autorité Spring Security
                // Le préfixe "ROLE_" est obligatoire pour que hasRole("ADMIN") fonctionne dans SecurityConfig
                // Exemple : "ADMIN" → "ROLE_ADMIN" → reconnu par .hasRole("ADMIN")
        );
    }
}