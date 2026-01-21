package com.example.order_management_api.service;

import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.api.UpdateProductRequest;
import com.example.order_management_api.api.UpdateStockRequest;
import com.example.order_management_api.exception.InsufficientStockException;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.repository.InventoryRepository;
import com.example.order_management_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        boolean active = request.active() != null ? request.active() : true;

        Product product = new Product(
                request.name(),
                request.price(),
                active
        );

        Product saved = productRepository.save(product);

        Inventory inventory = new Inventory(
                saved,
                request.initialStock(),
                0
        );

        inventoryRepository.saveAndFlush(inventory);

        return toResponse(saved, inventory);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Inventory inventory = inventoryRepository.findByProduct_Id(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(product, inventory);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(Boolean active) {
        return productRepository.findAll().stream()
                .filter(p -> active == null || p.isActive() == active)
                .map(p -> {
                    Inventory inv = inventoryRepository.findByProduct_Id(p.getId())
                            .orElseThrow();
                    return toResponse(p, inv);
                })
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }

        Inventory inventory = inventoryRepository.findByProduct_Id(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(product, inventory);
    }

    @Transactional
    public ProductResponse updateStock(UUID productId, UpdateStockRequest request) {
        Inventory inventory = inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (request.delta() != null) {
            int newAvailable = inventory.getAvailable() + request.delta();
            if (newAvailable < 0) {
                throw new InsufficientStockException(request.delta(), inventory.getAvailable());
            }
            inventory.setAvailable(newAvailable);
        } else {
            if (request.setTo() < 0) {
                throw new InsufficientStockException(request.setTo(), inventory.getAvailable());
            }
            inventory.setAvailable(request.setTo());
        }

        Product product = inventory.getProduct();
        return toResponse(product, inventory);
    }

    private ProductResponse toResponse(Product product, Inventory inventory) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.isActive(),
                inventory.getAvailable(),
                inventory.getReserved(),
                product.getCreatedAt()
        );
    }
}
