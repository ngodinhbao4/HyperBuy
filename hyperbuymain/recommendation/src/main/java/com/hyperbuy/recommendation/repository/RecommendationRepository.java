package com.hyperbuy.recommendation.repository;

import com.hyperbuy.recommendation.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);
}
