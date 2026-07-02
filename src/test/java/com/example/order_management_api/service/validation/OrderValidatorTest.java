package com.example.order_management_api.service.validation;

import com.example.order_management_api.exception.InactiveProductException;
import com.example.order_management_api.exception.ProductNotFoundException;
import com.example.order_management_api.model.Product;
import com.example.order_management_api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderValidatorTest {

    private ProductRepository productRepository;
    private OrderValidator orderValidator;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        orderValidator = new OrderValidator(productRepository);
    }

    @Test
    void returnsProductWhenActive() {
        UUID productId = UUID.randomUUID();
        Product product = new Product("Milk", BigDecimal.valueOf(3.99), true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Product result = orderValidator.validateOrderable(productId);

        assertThat(result).isSameAs(product);
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderValidator.validateOrderable(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void throwsWhenProductIsInactive() {
        UUID productId = UUID.randomUUID();
        Product product = new Product("Milk", BigDecimal.valueOf(3.99), false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderValidator.validateOrderable(productId))
                .isInstanceOf(InactiveProductException.class);
    }
}
