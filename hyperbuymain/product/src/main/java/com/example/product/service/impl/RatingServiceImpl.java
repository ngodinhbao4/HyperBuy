package com.example.product.service.impl;

import com.example.product.client.OrderClient;
import com.example.product.dto.request.RatingRequest;
import com.example.product.dto.response.RatingResponse;
import com.example.product.dto.response.RatingSummaryResponse;
import com.example.product.entity.Product;
import com.example.product.entity.Rating;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.RatingRepository;
import com.example.product.service.RatingService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;




import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;
    private final OrderClient orderClient;

    @Override
    @Transactional
    public RatingResponse createOrUpdateRating(Long productId, String username, RatingRequest request) {
        // 1. kiểm tra product tồn tại
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // 2. kiểm tra đã mua chưa (gọi order-service)
        Boolean purchased = orderClient.hasPurchased(username, productId);
        if (purchased == null || !purchased) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá sau khi đã mua sản phẩm này");
        }

        // 3. tìm rating cũ của user cho sản phẩm này (nếu có thì update)
        Rating rating = ratingRepository.findByProductIdAndUsername(productId, username)
                .orElseGet(() -> {
                    Rating r = new Rating();
                    r.setProduct(product);
                    r.setUsername(username);
                    return r;
                });

        rating.setRatingValue(request.getRatingValue());
        rating.setComment(request.getComment());

        Rating saved = ratingRepository.save(rating);

        return RatingResponse.builder()
                .id(saved.getId())
                .productId(productId)
                .username(saved.getUsername())
                .ratingValue(saved.getRatingValue())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public List<RatingResponse> getRatingsForProduct(Long productId) {
        return ratingRepository.findByProductId(productId)
                .stream()
                .map(r -> RatingResponse.builder()
                        .id(r.getId())
                        .productId(productId)
                        .username(r.getUsername())
                        .ratingValue(r.getRatingValue())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public RatingSummaryResponse getRatingSummaryForProduct(Long productId) {
        List<Rating> ratings = ratingRepository.findByProductId(productId);
        if (ratings.isEmpty()) {
            return RatingSummaryResponse.builder()
                    .productId(productId)
                    .averageRating(0.0)
                    .totalRatings(0)
                    .build();
        }

        IntSummaryStatistics stats = ratings.stream()
                .mapToInt(Rating::getRatingValue)
                .summaryStatistics();

        long count1 = ratings.stream().filter(r -> r.getRatingValue() == 1).count();
        long count2 = ratings.stream().filter(r -> r.getRatingValue() == 2).count();
        long count3 = ratings.stream().filter(r -> r.getRatingValue() == 3).count();
        long count4 = ratings.stream().filter(r -> r.getRatingValue() == 4).count();
        long count5 = ratings.stream().filter(r -> r.getRatingValue() == 5).count();

        double avg = stats.getAverage();

        return RatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(avg)
                .totalRatings(stats.getCount())
                .count1Star(count1)
                .count2Star(count2)
                .count3Star(count3)
                .count4Star(count4)
                .count5Star(count5)
                .build();
    }

    @Override
    public List<RatingResponse> getRatingsByUser(String username) {
        return ratingRepository.findByUsername(username)
                .stream()
                .map(r -> RatingResponse.builder()
                        .id(r.getId())
                        .productId(r.getProduct().getId())
                        .username(r.getUsername())
                        .ratingValue(r.getRatingValue())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
        @Transactional
        public void deleteMyRating(Long productId, Long ratingId, String username) {
            Rating rating = ratingRepository.findById(ratingId)
                    .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại"));

            // ✅ Kiểm tra đánh giá có thuộc đúng sản phẩm không
            if (!rating.getProduct().getId().equals(productId)) {
                throw new RuntimeException("Đánh giá không thuộc sản phẩm này");
            }

            // ✅ Chỉ cho phép chính chủ xóa
        if (!rating.getUsername().equals(username)) {
                throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này");
        }

            ratingRepository.delete(rating);
        }
}
