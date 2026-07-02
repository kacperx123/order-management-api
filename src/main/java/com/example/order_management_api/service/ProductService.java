package com.example.order_management_api.service;

import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.api.UpdateProductRequest;
import com.example.order_management_api.api.UpdateStockRequest;
import com.example.order_management_api.event.model.ProductCreatedEvent;
import com.example.order_management_api.event.model.StockAdjustedEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.mapper.ProductMapper;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.repository.InventoryRepository;
import com.example.order_management_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final DomainEventPublisher domainEventPublisher;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        boolean active = request.active() != null ? request.active() : true;

        Product saved = productRepository.save(new Product(
                request.name(),
                request.price(),
                active
        ));

        Inventory inventory = new Inventory(saved, request.initialStock(), 0);
        inventoryRepository.saveAndFlush(inventory);

        domainEventPublisher.publish(ProductCreatedEvent.now(saved.getId(), saved.getName(), saved.getPrice()));
        domainEventPublisher.publish(StockAdjustedEvent.now(saved.getId(), 0, inventory.getAvailable(), inventory.getReserved()));

        return productMapper.toResponse(saved, inventory);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return productMapper.toResponse(findProduct(id), inventoryService.getInventory(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(Boolean active) {
        List<Product> products = active == null
                ? productRepository.findAll()
                : productRepository.findByActive(active);

        // Fetch all inventories in one query instead of one per product.
        Map<UUID, Inventory> inventoriesByProductId = inventoryRepository
                .findByProduct_IdIn(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), Function.identity()));

        return products.stream()
                .map(p -> {
                    Inventory inventory = inventoriesByProductId.get(p.getId());
                    if (inventory == null) {
                        throw new ProductNotFoundException(p.getId());
                    }
                    return productMapper.toResponse(p, inventory);
                })
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findProduct(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }

        return productMapper.toResponse(product, inventoryService.getInventory(id));
    }

    @Transactional
    public ProductResponse updateStock(UUID productId, UpdateStockRequest request) {
        Inventory inventory = inventoryService.adjust(productId, request);
        return productMapper.toResponse(inventory.getProduct(), inventory);
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
