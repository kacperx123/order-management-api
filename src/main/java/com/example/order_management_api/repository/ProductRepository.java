package com.example.order_management_api.repository;

import com.example.order_management_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActive(boolean active);
}
