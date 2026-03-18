package com.example.cartservice.repository;

import com.example.cartservice.entity.Cart;
import com.example.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProductId(Cart cart, String productId);
    void deleteByCart_IdAndProductId(String cartId, String productId);
    void deleteAllByCart_Id(String cartId);
}
