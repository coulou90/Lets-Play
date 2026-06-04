package lets_play.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utilitaire centralisé pour la gestion des tokens JWT.
 * Responsabilités :
 *   - Générer un token signé lors de la connexion
 *   - Extraire les claims (email, rôle) depuis un token
 *   - Valider un token (signature + expiration)
 *
 * Les paramètres de configuration (secret, durée) sont externalisés
 * dans application.properties pour ne pas être codés en dur.
 */
@Component // Bean Spring — injectable dans JwtFilter et AuthController
public class JwtUtil {

    // Clé secrète lue depuis application.properties : app.jwt.secret
    // Utilisée pour signer et vérifier les tokens — ne doit JAMAIS être commitée en dur dans le code
    // ⚠️ En production : utiliser une variable d'environnement ou un vault (ex: HashiCorp Vault)
    @Value("${app.jwt.secret}")
    private String secret;

    // Durée de validité du token en millisecondes, lue depuis application.properties : app.jwt.expiration
    // Exemple : 86400000 = 24 heures
    @Value("${app.jwt.expiration}")
    private long expiration;

    /**
     * Construit la clé de signature HMAC-SHA à partir du secret.
     * Keys.hmacShaKeyFor() vérifie que le secret est suffisamment long
     * pour HS256 (minimum 256 bits / 32 caractères).
     * Appelée en interne à chaque opération de signature ou de parsing.
     *
     * @return Clé cryptographique prête à l'emploi
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes()); // Convertit le secret String en clé HMAC-SHA256
    }

    /**
     * Génère un token JWT signé pour un utilisateur authentifié.
     * Structure du token (payload) :
     *   - sub   : email de l'utilisateur (identifiant standard JWT)
     *   - role  : rôle ("USER" ou "ADMIN") — claim personnalisé
     *   - iat   : date d'émission (issued at)
     *   - exp   : date d'expiration = maintenant + expiration
     *
     * @param email Email de l'utilisateur (stocké dans le claim "sub")
     * @param role  Rôle de l'utilisateur (stocké dans le claim "role")
     * @return Token JWT compacté sous forme de String (format : header.payload.signature)
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)                                              // Claim standard "sub" : identifiant du porteur
                .claim("role", role)                                            // Claim personnalisé : rôle embarqué dans le token
                .setIssuedAt(new Date())                                        // Claim "iat" : horodatage de création
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Claim "exp" : horodatage d'expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)            // Signature HMAC-SHA256 avec la clé secrète
                .compact();                                                     // Sérialise en String Base64 : "xxxxx.yyyyy.zzzzz"
    }

    /**
     * Extrait l'email (claim "sub") depuis un token validé.
     * Utilisé dans JwtFilter pour identifier l'utilisateur
     * et dans ProductController pour récupérer l'userId.
     *
     * @param token Token JWT brut (sans "Bearer ")
     * @return Email de l'utilisateur
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject(); // getSubject() lit le claim standard "sub"
    }

    /**
     * Extrait le rôle (claim "role") depuis un token validé.
     * Utilisé dans ProductController pour vérifier si l'utilisateur
     * est propriétaire ou ADMIN avant une modification/suppression.
     *
     * @param token Token JWT brut (sans "Bearer ")
     * @return Rôle de l'utilisateur ("USER" ou "ADMIN")
     */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class); // Lecture du claim personnalisé "role" typé en String
    }

    /**
     * Vérifie si un token est valide : signature correcte ET non expiré.
     * Repose sur getClaims() qui lève une exception si le token est invalide.
     * Utilisé dans JwtFilter avant toute extraction de claims.
     *
     * @param token Token JWT brut (sans "Bearer ")
     * @return true si valide, false dans tous les cas d'erreur
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token); // Si cette ligne ne lève pas d'exception → token valide
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException couvre tous les cas d'erreur JWT :
            //   - ExpiredJwtException     → token expiré
            //   - MalformedJwtException   → format invalide (pas 3 segments Base64)
            //   - SignatureException      → signature ne correspond pas au secret
            //   - UnsupportedJwtException → algorithme non supporté
            // IllegalArgumentException   → token null ou vide
            return false; // Token rejeté — pas de détail exposé volontairement
        }
    }

    /**
     * Parse le token et retourne l'ensemble des claims (payload décodé).
     * Méthode privée centrale : toute extraction de données passe par ici.
     * Le parsing vérifie simultanément la signature et l'expiration.
     *
     * @param token Token JWT brut (sans "Bearer ")
     * @return Claims contenant sub, role, iat, exp
     * @throws JwtException si le token est invalide ou expiré
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // Configure la clé pour vérifier la signature
                .build()                        // Construit le parser immuable
                .parseClaimsJws(token)          // Parse ET valide le token (signature + expiration)
                .getBody();                     // Retourne uniquement le payload (claims)
    }
}