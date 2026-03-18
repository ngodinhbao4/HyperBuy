package com.example.cartservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true) // Mỗi user có 1 cart
    private String userId; // ID từ UserService

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Cart() {
        this.id = UUID.randomUUID().toString(); // Tự tạo UUID khi khởi tạo
    }

    public Cart(String userId) {
        this(); // Gọi constructor mặc định để tạo ID
        this.userId = userId;
    }

    // Helper methods
    public void addItem(CartItem item) {
        this.items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        this.items.remove(item);
        item.setCart(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return Objects.equals(id, cart.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}