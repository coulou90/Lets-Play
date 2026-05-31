package lets_play.service;

import lets_play.exception.ResourceNotFoundException;
import lets_play.model.Product;
import lets_play.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

    public Product createProduct(Product product, String userId) {
        product.setUserId(userId);
        return productRepository.save(product);
    }

    public Product updateProduct(String id, Product updatedProduct, String userId, String role) {
        Product product = getProductById(id);
        if (!product.getUserId().equals(userId) && !role.equals("ADMIN")) {
            throw new AccessDeniedException("Accès refusé");
        }
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        return productRepository.save(product);
    }

    public void deleteProduct(String id, String userId, String role) {
        Product product = getProductById(id);
        if (!product.getUserId().equals(userId) && !role.equals("ADMIN")) {
            throw new AccessDeniedException("Accès refusé");
        }
        productRepository.deleteById(id);
    }
}