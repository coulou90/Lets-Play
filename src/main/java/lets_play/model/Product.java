package lets_play.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entité représentant un produit dans la base de données MongoDB.
 * Chaque instance correspond à un document dans la collection "products".
 *
 * Structure du document MongoDB :
 * {
 *   "_id"         : "664b2e...",   (ObjectId généré par MongoDB)
 *   "name"        : "Manette PS5",
 *   "description" : "Manette DualSense sans fil",
 *   "price"       : 69.99,
 *   "userId"      : "664a1f..."    (référence vers _id dans la collection "users")
 * }
 */
@Data            // Lombok : génère getters, setters, equals(), hashCode(), toString()
@NoArgsConstructor  // Lombok : constructeur vide — requis par MongoDB pour désérialiser les documents
@AllArgsConstructor // Lombok : constructeur avec tous les champs — utile pour les tests
@Document(collection = "products") // Mappe cette classe sur la collection MongoDB "products"
public class Product {

    @Id // Identifiant primaire mappé sur "_id" dans MongoDB
        // Généré automatiquement par MongoDB si non fourni à la création
    private String id;

    private String name;        // Nom du produit (ex: "Manette PS5")

    private String description; // Description détaillée du produit

    private Double price;       // Prix du produit en Double pour supporter les décimales (ex: 69.99)
                                //  En production, préférer BigDecimal pour éviter
                                // les erreurs d'arrondi sur les calculs financiers

    private String userId;      // Référence vers l'id du propriétaire dans la collection "users"
                                // Relation manuel MongoDB (pas de @DBRef) :
                                // plus performant car pas de jointure automatique,
                                // mais la cohérence doit être gérée côté applicatif
                                // Utilisé dans ProductService pour vérifier si l'utilisateur
                                // est bien le propriétaire avant toute modification/suppression
}