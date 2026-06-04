package lets_play.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lets_play.exception.ResourceNotFoundException;
import lets_play.model.Product;
import lets_play.repository.UserRepository;
import lets_play.security.JwtUtil;
import lets_play.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST gérant les opérations CRUD sur les produits.
 * Certains endpoints sont publics, d'autres nécessitent un token JWT valide.
 *
 * Règles d'accès :
 *   - GET  /api/products        → public
 *   - GET  /api/products/{id}   → public
 *   - POST /api/products        → tout utilisateur authentifié
 *   - PUT  /api/products/{id}   → propriétaire du produit ou ADMIN
 *   - DELETE /api/products/{id} → propriétaire du produit ou ADMIN
 */
@Tag(name = "Produits", description = "Gestion des produits") // Groupe Swagger UI
@RestController
@RequestMapping("/api/products") // Préfixe commun à tous les endpoints de ce contrôleur
@RequiredArgsConstructor         // Lombok : injection par constructeur des champs 'final'
public class ProductController {

    private final ProductService productService;   // Contient la logique métier des produits
    private final UserRepository userRepository;   // Nécessaire pour résoudre l'email → userId depuis le JWT
    private final JwtUtil jwtUtil;                 // Extraction des claims (email, rôle) depuis le token JWT

    // -------------------------------------------------------------------------
    // ENDPOINTS PUBLICS
    // -------------------------------------------------------------------------

    /**
     * Retourne la liste complète des produits.
     * Accessible sans authentification (configuré dans SecurityConfig).
     */
    @Operation(summary = "Liste tous les produits", description = "Public - aucune authentification requise")
    @GetMapping // GET /api/products
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts()); // 200 OK + liste JSON
    }

    /**
     * Retourne un produit par son identifiant MongoDB.
     * Lance ResourceNotFoundException (→ 404) si l'id est inconnu.
     */
    @Operation(summary = "Recuperer un produit", description = "Public - aucune authentification requise")
    @GetMapping("/{id}") // GET /api/products/{id}
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        // @PathVariable : lie le segment {id} de l'URL au paramètre 'id'
        return ResponseEntity.ok(productService.getProductById(id)); // 200 OK + produit JSON
    }

    // -------------------------------------------------------------------------
    // ENDPOINTS PROTÉGÉS (JWT requis)
    // -------------------------------------------------------------------------

    /**
     * Crée un nouveau produit et l'associe à l'utilisateur authentifié.
     * Le propriétaire est déduit du token JWT (pas passé manuellement par le client).
     *
     * @param product    Corps JSON du produit à créer
     * @param authHeader Header "Authorization: Bearer <token>"
     * @return 201 Created + le produit créé avec son id MongoDB
     */
    @Operation(summary = "Creer un produit", description = "Authentifie uniquement")
    @SecurityRequirement(name = "Bearer Authentication") // Affiche le cadenas 🔒 sur cet endpoint dans Swagger UI
    @PostMapping // POST /api/products
    public ResponseEntity<Product> createProduct(
            @RequestBody Product product,
            @RequestHeader("Authorization") String authHeader) { // Récupère le header Authorization de la requête HTTP

        // Extraction du token : supprime le préfixe "Bearer " (7 caractères) pour isoler le token brut
        String token = authHeader.substring(7);

        // Extraction de l'email depuis les claims du JWT (sans requête base)
        String email = jwtUtil.extractEmail(token);

        // Résolution de l'userId depuis l'email : nécessaire pour lier le produit à son propriétaire
        // orElseThrow → lance ResourceNotFoundException si l'email n'existe pas en base (cas anormal)
        String userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"))
                .getId();

        // Délègue la création au service, en lui passant l'userId pour associer le produit au propriétaire
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(product, userId)); // 201 Created + produit persisté
    }

    /**
     * Met à jour un produit existant.
     * Seul le propriétaire du produit ou un ADMIN peut effectuer cette opération.
     * La vérification des droits est déléguée au ProductService.
     *
     * @param id         Identifiant MongoDB du produit à modifier
     * @param product    Corps JSON avec les nouvelles valeurs
     * @param authHeader Header "Authorization: Bearer <token>"
     * @return 200 OK + produit mis à jour, ou 403 Forbidden si non autorisé
     */
    @Operation(summary = "Modifier un produit", description = "Proprietaire ou Admin uniquement")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}") // PUT /api/products/{id}
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @RequestBody Product product,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7); // Supprime "Bearer "

        String email = jwtUtil.extractEmail(token); // Email du demandeur
        String role  = jwtUtil.extractRole(token);  // Rôle du demandeur (USER ou ADMIN)

        // Résolution userId : le service en a besoin pour vérifier si l'utilisateur est bien le propriétaire
        String userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"))
                .getId();

        // Le service vérifie les droits (propriétaire OU admin) avant d'appliquer la modification
        return ResponseEntity.ok(productService.updateProduct(id, product, userId, role)); // 200 OK
    }

    /**
     * Supprime un produit existant.
     * Seul le propriétaire du produit ou un ADMIN peut effectuer cette opération.
     * La vérification des droits est déléguée au ProductService.
     *
     * @param id         Identifiant MongoDB du produit à supprimer
     * @param authHeader Header "Authorization: Bearer <token>"
     * @return 200 OK + message de confirmation, ou 403 Forbidden si non autorisé
     */
    @Operation(summary = "Supprimer un produit", description = "Proprietaire ou Admin uniquement")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}") // DELETE /api/products/{id}
    public ResponseEntity<?> deleteProduct(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7); // Supprime "Bearer "

        String email  = jwtUtil.extractEmail(token); // Email du demandeur
        String role   = jwtUtil.extractRole(token);  // Rôle du demandeur (USER ou ADMIN)

        String userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"))
                .getId();

        // Le service vérifie les droits puis supprime — ne retourne rien (void)
        productService.deleteProduct(id, userId, role);

        // 200 OK + message de confirmation (pas de 204 No Content pour rester cohérent avec les autres réponses)
        return ResponseEntity.ok(Map.of("message", "Produit supprime"));
    }
}