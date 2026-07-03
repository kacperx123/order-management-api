package com.example.order_management_api.controller;

import com.example.order_management_api.api.InventoryResponse;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<InventoryResponse> listInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                inventory.getAvailable(),
                inventory.getReserved(),
                inventory.getVersion()
        );
    }
}
