package com.hyperbuy.recommendation.service;

import com.hyperbuy.recommendation.model.Recommendation;
import com.hyperbuy.recommendation.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RecommendationService {
    private final RecommendationRepository repository;
    private final RecommendationGenerator generator;

    public RecommendationService(RecommendationRepository repository, RecommendationGenerator generator) {
        this.repository = repository;
        this.generator = generator;
    }

    public List<Recommendation> getUserRecommendations(Long userId) {
        // Lấy từ DB nếu đã có
        List<Recommendation> existing = repository.findByUserIdOrderByScoreDesc(userId);
        if (!existing.isEmpty()) return existing;

        // Nếu chưa có thì tạo mới dựa vào hành vi
        List<Recommendation> generated = generator.generateForUser(userId);
        repository.saveAll(generated);
        return generated;
    }
}
