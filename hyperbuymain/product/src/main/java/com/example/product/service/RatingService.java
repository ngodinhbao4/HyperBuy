package com.example.product.service;

import com.example.product.dto.request.RatingRequest;
import com.example.product.dto.response.RatingResponse;
import com.example.product.dto.response.RatingSummaryResponse;

import java.util.List;

public interface RatingService {

    RatingResponse createOrUpdateRating(Long productId, String username, RatingRequest request);

    List<RatingResponse> getRatingsForProduct(Long productId);

    RatingSummaryResponse getRatingSummaryForProduct(Long productId);

    List<RatingResponse> getRatingsByUser(String username);

    void deleteMyRating(Long productId, Long ratingId, String username);
}
