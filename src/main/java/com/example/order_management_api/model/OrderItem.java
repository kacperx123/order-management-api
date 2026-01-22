package com.example.order_management_api.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productNameSnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPriceAtPurchase;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @Setter(AccessLevel.PACKAGE)
    private Order order;

    public OrderItem(UUID productId, String productNameSnapshot, BigDecimal unitPriceAtPurchase, int quantity) {
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPriceAtPurchase = unitPriceAtPurchase;
        this.quantity = quantity;
    }
}
