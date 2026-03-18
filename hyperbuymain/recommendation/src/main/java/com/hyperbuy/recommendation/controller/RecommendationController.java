package com.hyperbuy.recommendation.controller;

import com.hyperbuy.recommendation.model.Recommendation;
import com.hyperbuy.recommendation.service.RecommendationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public List<Recommendation> getRecommendations(@PathVariable Long userId) {
        return service.getUserRecommendations(userId);
    }
}
