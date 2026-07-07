package com.example.order_management_api.service;

import com.example.order_management_api.api.UpdateStockRequest;
import com.example.order_management_api.event.model.StockAdjustedEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.InsufficientStockException;
import com.example.order_management_api.exception.OutOfStockException;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single place that mutates {@link Inventory}. Methods are meant to be called
 * inside the caller's transaction (changes propagate via dirty checking).
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final SimulatedProcessingDelay simulatedProcessingDelay;

    public Inventory reserve(UUID productId, String productName, int quantity) {
        Inventory inventory = getInventory(productId);

        int available = inventory.getAvailable();
        if (available < quantity) {
            throw new OutOfStockException(productName, quantity, available);
        }

        inventory.setAvailable(available - quantity);

        simulatedProcessingDelay.apply();

        return inventory;
    }

    public void release(UUID productId, int quantity) {
        Inventory inventory = getInventory(productId);

        int previous = inventory.getAvailable();
        inventory.setAvailable(previous + quantity);

        domainEventPublisher.publish(
                StockAdjustedEvent.now(productId, previous, inventory.getAvailable(), inventory.getReserved())
        );
    }

    public Inventory adjust(UUID productId, UpdateStockRequest request) {
        Inventory inventory = getInventory(productId);

        int previous = inventory.getAvailable();
        int attempted = request.delta() != null ? request.delta() : request.setTo();
        int newAvailable = request.delta() != null ? previous + request.delta() : request.setTo();

        if (newAvailable < 0) {
            throw new InsufficientStockException(attempted, previous);
        }

        inventory.setAvailable(newAvailable);

        domainEventPublisher.publish(
                StockAdjustedEvent.now(productId, previous, newAvailable, inventory.getReserved())
        );

        return inventory;
    }

    public Inventory getInventory(UUID productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
