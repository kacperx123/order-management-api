package com.example.order_management_api.mapper;

import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product, Inventory inventory) {
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
