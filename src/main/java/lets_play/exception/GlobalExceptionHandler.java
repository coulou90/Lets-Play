package lets_play.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour toute l'application.
 * Intercepte les exceptions lancées par n'importe quel contrôleur
 * et les transforme en réponses HTTP structurées et cohérentes.
 *
 * Sans cette classe, Spring retournerait ses erreurs par défaut
 * (Whitelabel Error Page) — peu lisibles pour un client API.
 *
 * Hiérarchie des handlers (du plus spécifique au plus générique) :
 *   NoResourceFoundException   → 404 vide   (ressources statiques)
 *   ResourceNotFoundException  → 404 JSON   (entité métier introuvable)
 *   AccessDeniedException      → 403 JSON   (droits insuffisants)
 *   MethodArgumentNotValidException → 400 JSON (validation @Valid)
 *   IllegalStateException      → 409 JSON   (conflit métier)
 *   Exception                  → 500 JSON   (erreur inattendue)
 */
@RestControllerAdvice // Combine @ControllerAdvice + @ResponseBody :
                      // intercepte les exceptions de tous les @RestController
                      // et sérialise automatiquement les réponses en JSON
public class GlobalExceptionHandler {

    /**
     * Gère les requêtes vers des ressources statiques inexistantes.
     * Cas typique : Swagger tente de charger des fichiers JS/CSS au démarrage.
     * Retourne un 404 vide (sans corps JSON) pour ne pas polluer les logs
     * avec des erreurs liées à l'infrastructure de documentation.
     */
    @ExceptionHandler(NoResourceFoundException.class) // Cible uniquement cette exception
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build(); // 404 sans corps — Void indique l'absence de body
    }

    /**
     * Gère les erreurs métier "ressource introuvable".
     * Lancée manuellement dans les services via :
     *   throw new ResourceNotFoundException("Produit introuvable")
     * Le message de l'exception est directement retourné au client.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiException> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiException(
                        HttpStatus.NOT_FOUND.value(), // 404 en entier (ex: 404)
                        ex.getMessage(),              // Message métier défini à l'endroit du throw
                        LocalDateTime.now()           // Horodatage de l'erreur pour le debugging
                ));
    }

    /**
     * Gère les tentatives d'accès à des ressources sans les droits suffisants.
     * Lancée automatiquement par Spring Security quand un utilisateur
     * authentifié tente d'accéder à un endpoint interdit (ex: USER sur /api/users/**).
     * ⚠️ Le message de l'exception Spring est volontairement ignoré
     * au profit d'un message générique pour ne pas exposer la logique interne.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiException> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiException(
                        HttpStatus.FORBIDDEN.value(), // 403
                        "Acces refuse",               // Message générique — ne révèle pas la règle de sécurité
                        LocalDateTime.now()
                ));
    }

    /**
     * Gère les erreurs de validation des DTOs annotés avec @Valid.
     * Lancée automatiquement par Spring quand une contrainte (@NotBlank, @Email, @Pattern...)
     * n'est pas respectée dans le corps de la requête.
     *
     * Retourne une Map<champ, message> pour identifier précisément
     * quel champ du formulaire/JSON est invalide.
     * Exemple de réponse :
     * {
     *   "email": "Format email invalide",
     *   "password": "Le mot de passe doit contenir au moins 8 caractères..."
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        // getBindingResult() contient tous les résultats de validation
        // getFieldErrors() retourne la liste des champs en erreur
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            // error.getField()          → nom du champ Java en erreur (ex: "email")
            // error.getDefaultMessage() → message défini dans l'annotation (ex: "Format email invalide")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors); // 400 + map des erreurs
    }

    /**
     * Gère les conflits métier (état invalide de l'application).
     * Lancée manuellement dans les services pour signaler un conflit :
     *   throw new IllegalStateException("Ce produit appartient à un autre utilisateur")
     * Le message de l'exception est directement retourné au client.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiException> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiException(
                        HttpStatus.CONFLICT.value(), // 409
                        ex.getMessage(),             // Message métier défini à l'endroit du throw
                        LocalDateTime.now()
                ));
    }

    /**
     * Handler de dernier recours : attrape toute exception non gérée par les handlers précédents.
     * ⚠️ Le message original de l'exception est volontairement masqué ("Une erreur interne est survenue")
     * pour ne pas exposer des détails techniques sensibles (stack trace, nom de classe, requête SQL...)
     * qui pourraient être exploités par un attaquant.
     */
    @ExceptionHandler(Exception.class) // Attrape TOUTES les exceptions non interceptées plus haut
    public ResponseEntity<ApiException> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
                        "Une erreur interne est survenue",        // Message générique — ne révèle rien
                        LocalDateTime.now()
                ));
    }
}