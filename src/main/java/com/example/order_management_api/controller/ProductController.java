package com.example.order_management_api.controller;

import com.example.order_management_api.api.CreateProductRequest;
import com.example.order_management_api.api.ProductResponse;
import com.example.order_management_api.api.UpdateProductRequest;
import com.example.order_management_api.api.UpdateStockRequest;
import com.example.order_management_api.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse created = productService.createProduct(request);

        return ResponseEntity
                .created(URI.create("/products/" + created.id()))
                .body(created);
    }

    @GetMapping
    public List<ProductResponse> listProducts(
            @RequestParam(required = false) Boolean active
    ) {
        return productService.listProducts(active);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @PatchMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @PostMapping("/{id}/stock")
    public ProductResponse updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return productService.updateStock(id, request);
    }
}
