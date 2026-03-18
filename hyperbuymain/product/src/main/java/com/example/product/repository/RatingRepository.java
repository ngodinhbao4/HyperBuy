package com.example.product.repository;

import com.example.product.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByProductId(Long productId);

    Optional<Rating> findByProductIdAndUsername(Long productId, String username);

    List<Rating> findByUsername(String username);

    
}
