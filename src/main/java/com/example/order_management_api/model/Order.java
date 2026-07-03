package com.example.order_management_api.model;

import com.example.order_management_api.exception.InvalidOrderStatusTransitionException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    private String customerEmail;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order(UUID id, String customerEmail, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Order newOrder(User user) {
        Order order = new Order(UUID.randomUUID(), user.getEmail(), OrderStatus.CREATED, Instant.now());
        order.user = user;
        return order;
    }

    public boolean isOwnedBy(UUID userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void pay() {
        transitionTo(OrderStatus.PAID);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    private void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStatusTransitionException(status, target);
        }
        this.status = target;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Order order = (Order) o;
        return getId() != null && Objects.equals(getId(), order.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

}
