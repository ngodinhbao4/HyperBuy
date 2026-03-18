package com.example.product.controller;

import com.example.product.dto.response.ApiResponse;
import com.example.product.dto.response.ProductResponse;
import com.example.product.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    // ✅ Gợi ý cho user hiện tại (đã đăng nhập)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyRecommendations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "12") int limit
    ) {
        String username = jwt.getSubject(); // subject = username

        List<ProductResponse> products = aiRecommendationService.getRecommendationsForUser(username, limit);

        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .code(1000)
                .message("Gợi ý AI cho user: " + username)
                .result(products)
                .build();

        return ResponseEntity.ok(response);
    }

    // ✅ Gợi ý cho khách (chưa đăng nhập) -> popular / trending
        @GetMapping("/guest")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> getGuestRecommendations(
                @RequestParam(defaultValue = "12") int limit
        ) {
                List<ProductResponse> products = aiRecommendationService.getRecommendationsForGuest(limit);

                ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                        .code(1000)
                        .message("Gợi ý AI cho khách chưa đăng nhập")
                        .result(products)
                        .build();

                return ResponseEntity.ok(response);
        }

        @GetMapping("/similar/{productId}")
        public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
                @PathVariable Long productId,
                @RequestParam(defaultValue = "10") int limit
        ) {
        List<ProductResponse> products = aiRecommendationService.getSimilarProducts(productId, limit);

        ApiResponse<List<ProductResponse>> response = ApiResponse.<List<ProductResponse>>builder()
                .code(1000)
                .message("Sản phẩm tương tự cho productId=" + productId)
                .result(products)
                .build();

        return ResponseEntity.ok(response);
        }
    
}
