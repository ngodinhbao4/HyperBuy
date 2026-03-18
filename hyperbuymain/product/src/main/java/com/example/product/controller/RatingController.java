package com.example.product.controller;

import com.example.product.dto.request.RatingRequest;
import com.example.product.dto.response.ApiResponse;
import com.example.product.dto.response.RatingResponse;
import com.example.product.dto.response.RatingSummaryResponse;
import com.example.product.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}")
@RequiredArgsConstructor
@Slf4j
public class RatingController {

    private final RatingService ratingService;

    // 🟢 Tạo/sửa rating – chỉ user đã mua mới gọi được
    @PostMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> createOrUpdateRating(
            @PathVariable Long productId,
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String username = jwt.getSubject(); // vì bạn set subject = username trong AuthenticationService
        RatingResponse result = ratingService.createOrUpdateRating(productId, username, request);

        ApiResponse<RatingResponse> response = ApiResponse.<RatingResponse>builder()
                .code(1000)
                .message("Đánh giá sản phẩm thành công")
                .result(result)
                .build();

        return ResponseEntity.ok(response);
    }

    // 🟢 Xem danh sách đánh giá của sản phẩm (public)
    @GetMapping("/ratings")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getRatings(
            @PathVariable Long productId
    ) {
        List<RatingResponse> ratings = ratingService.getRatingsForProduct(productId);

        ApiResponse<List<RatingResponse>> response = ApiResponse.<List<RatingResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đánh giá thành công")
                .result(ratings)
                .build();

        return ResponseEntity.ok(response);
    }

    // 🟢 Xem tổng quan rating (average, count)
    @GetMapping("/rating-summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getRatingSummary(
            @PathVariable Long productId
    ) {
        RatingSummaryResponse summary = ratingService.getRatingSummaryForProduct(productId);

        ApiResponse<RatingSummaryResponse> response = ApiResponse.<RatingSummaryResponse>builder()
                .code(1000)
                .message("Lấy tổng quan đánh giá thành công")
                .result(summary)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/my/{ratingId}")
        public ResponseEntity<ApiResponse<Void>> deleteMyRating(
                @PathVariable Long productId,
                @PathVariable Long ratingId,
                @AuthenticationPrincipal Jwt jwt
        ) {
        String username = jwt.getSubject();

                ratingService.deleteMyRating(productId, ratingId, username);

            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .code(1000)
                    .message("Xóa đánh giá thành công")
                        .build();

            return ResponseEntity.ok(response);
        }               
}
