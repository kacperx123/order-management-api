package com.example.order_management_api.service;

import com.example.order_management_api.api.UpdateStockRequest;
import com.example.order_management_api.event.model.DomainEvent;
import com.example.order_management_api.event.model.StockAdjustedEvent;
import com.example.order_management_api.event.publisher.DomainEventPublisher;
import com.example.order_management_api.exception.InsufficientStockException;
import com.example.order_management_api.exception.OutOfStockException;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.model.Inventory;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private InventoryRepository inventoryRepository;
    private final List<DomainEvent> publishedEvents = new ArrayList<>();
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        publishedEvents.clear();
        DomainEventPublisher recordingPublisher = publishedEvents::add;
        inventoryService = new InventoryService(
                inventoryRepository,
                recordingPublisher,
                new SimulatedProcessingDelay(0)
        );
    }

    private UUID givenInventory(int available) {
        UUID productId = UUID.randomUUID();
        Product product = new Product("Milk", BigDecimal.valueOf(3.99), true);
        Inventory inventory = new Inventory(product, available, 0);
        when(inventoryRepository.findByProduct_Id(productId)).thenReturn(Optional.of(inventory));
        return productId;
    }

    @Test
    void reserveDecreasesAvailableStock() {
        UUID productId = givenInventory(10);

        Inventory inventory = inventoryService.reserve(productId, "Milk", 3);

        assertThat(inventory.getAvailable()).isEqualTo(7);
    }

    @Test
    void reserveThrowsWhenNotEnoughStock() {
        UUID productId = givenInventory(1);

        assertThatThrownBy(() -> inventoryService.reserve(productId, "Milk", 2))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    void reserveThrowsWhenInventoryMissing() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.findByProduct_Id(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserve(productId, "Milk", 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void releaseRestoresStockAndPublishesEvent() {
        UUID productId = givenInventory(7);

        inventoryService.release(productId, 3);

        Inventory inventory = inventoryRepository.findByProduct_Id(productId).orElseThrow();
        assertThat(inventory.getAvailable()).isEqualTo(10);
        assertThat(publishedEvents)
                .singleElement()
                .isInstanceOfSatisfying(StockAdjustedEvent.class, e -> {
                    assertThat(e.previousAvailable()).isEqualTo(7);
                    assertThat(e.newAvailable()).isEqualTo(10);
                });
    }

    @Test
    void adjustWithDeltaChangesStock() {
        UUID productId = givenInventory(10);

        Inventory inventory = inventoryService.adjust(productId, new UpdateStockRequest(-4, null));

        assertThat(inventory.getAvailable()).isEqualTo(6);
        assertThat(publishedEvents).hasSize(1);
    }

    @Test
    void adjustWithSetToOverwritesStock() {
        UUID productId = givenInventory(10);

        Inventory inventory = inventoryService.adjust(productId, new UpdateStockRequest(null, 25));

        assertThat(inventory.getAvailable()).isEqualTo(25);
    }

    @Test
    void adjustThrowsWhenResultWouldBeNegative() {
        UUID productId = givenInventory(3);

        assertThatThrownBy(() -> inventoryService.adjust(productId, new UpdateStockRequest(-5, null)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(publishedEvents).isEmpty();
    }
}
