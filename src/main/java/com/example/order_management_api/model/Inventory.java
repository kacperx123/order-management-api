package com.example.order_management_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product_id", columnNames = "product_id")
)
public class Inventory {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    @Version
    private Long version;

    protected Inventory() {
        // JPA
    }

    public Inventory(Product product, int available, int reserved) {
        this.product = product;
        this.available = available;
        this.reserved = reserved;
    }
}
