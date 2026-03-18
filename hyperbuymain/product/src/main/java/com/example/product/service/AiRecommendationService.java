package com.example.product.service;

import com.example.product.dto.response.ProductResponse;

import java.util.List;

public interface AiRecommendationService {
    List<ProductResponse> getRecommendationsForUser(String username, int limit);
    List<ProductResponse> getRecommendationsForGuest(int limit);
    List<ProductResponse> getSimilarProducts(Long productId, int limit);
}
