package com.example.cartservice.controller;

import com.example.cartservice.dto.CheckoutCartItemRequest;
import com.example.cartservice.dto.request.CartItemRequest;
import com.example.cartservice.dto.response.ApiResponse;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.exception.CartException;
import com.example.cartservice.exception.ErrorCodeCart;
import com.example.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    private String getAuthenticatedUserId(Jwt jwtPrincipal) {
        if (jwtPrincipal == null) {
            log.warn("JWT Principal is null. User not authenticated or token not provided correctly.");
            throw new CartException(ErrorCodeCart.USER_NOT_AUTHENTICATED);
        }
        String userId = jwtPrincipal.getSubject();
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("User ID (subject) is null or empty in JWT.");
            throw new CartException(ErrorCodeCart.USER_NOT_AUTHENTICATED, "User identifier not found in token.");
        }
        return userId;
    }

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<String>> getOrCreateCartIdForCurrentUser(@AuthenticationPrincipal Jwt jwtPrincipal) {
        String userId = getAuthenticatedUserId(jwtPrincipal);
        log.info("Request to get or create cart ID for authenticated user: {}", userId);
        String cartId = cartService.getOrCreateCartIdForUser(userId);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(HttpStatus.OK.value())
                .message("Cart ID retrieved or created successfully for current user.")
                .result(cartId)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCurrentUserCart(@AuthenticationPrincipal Jwt jwtPrincipal) {
        String userId = getAuthenticatedUserId(jwtPrincipal);
        // Lấy token JWT
        String token = "Bearer " + jwtPrincipal.getTokenValue();
        log.info("Request to get cart for authenticated user: {}", userId);

        // Truyền token vào CartService
        CartResponse cartDto = cartService.getCartByUserId(userId, token);
        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cart retrieved successfully for current user.")
                .result(cartDto)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/my-cart/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCurrentUsersCart(
            @AuthenticationPrincipal Jwt jwtPrincipal,
            @Valid @RequestBody CartItemRequest itemRequestDto) {
        String userId = getAuthenticatedUserId(jwtPrincipal);
        String token = "Bearer " + jwtPrincipal.getTokenValue();
        log.info("Request to add item to cart for authenticated user: {}. Item: {}", userId, itemRequestDto);

        CartResponse updatedCart = cartService.addItemToCart(userId, itemRequestDto, token);
        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Item added to current user's cart successfully.")
                .result(updatedCart)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/my-cart/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCurrentUsersCartItemQuantity(
            @AuthenticationPrincipal Jwt jwtPrincipal,
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        String userId = getAuthenticatedUserId(jwtPrincipal);
        log.info("Request to update quantity for product {} in cart of authenticated user {} to {}", productId, userId, quantity);

        CartResponse updatedCart = cartService.updateCartItemQuantity(userId, productId, quantity);
        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Current user's cart item quantity updated successfully.")
                .result(updatedCart)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/my-cart/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCurrentUsersCart(
            @AuthenticationPrincipal Jwt jwtPrincipal,
            @PathVariable String productId) {
        String userId = getAuthenticatedUserId(jwtPrincipal);
        log.info("Request to remove product {} from cart of authenticated user {}", productId, userId);

        CartResponse updatedCart = cartService.removeItemFromCart(userId, productId);
        ApiResponse<CartResponse> response = ApiResponse.<CartResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Item removed from current user's cart successfully.")
                .result(updatedCart)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/my-cart")
    public ResponseEntity<ApiResponse<Void>> clearCurrentUsersCart(@AuthenticationPrincipal Jwt jwtPrincipal) {
        String userId = getAuthenticatedUserId(jwtPrincipal);
        log.info("Request to clear cart for authenticated user: {}", userId);

        cartService.clearCart(userId);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Current user's cart cleared successfully.")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout-remove")
    public ResponseEntity<Void> removeItemsAfterCheckout(
            @RequestBody List<CheckoutCartItemRequest> items,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = authentication.getName(); // hoặc lấy từ JWT claim nếu em đang dùng UUID
        cartService.removeItemsAfterCheckout(userId, items);
        return ResponseEntity.ok().build();
    }
}