package com.example.order_management_api.service.validation;

import com.example.order_management_api.exception.InactiveProductException;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final ProductRepository productRepository;

    /**
     * Ensures the product can be ordered: it exists and is active.
     * Returns the product so callers can snapshot its name and price.
     */
    public Product validateOrderable(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!product.isActive()) {
            throw new InactiveProductException(productId);
        }

        return product;
    }
}
