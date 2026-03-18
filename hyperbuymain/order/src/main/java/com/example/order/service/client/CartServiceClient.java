package com.example.order.service.client;


import com.example.order.dto.CartDTO;
import com.example.order.dto.CartItemDTO;
import com.example.order.dto.response.CartApiResponse;
import com.example.order.dto.response.CartResponse;
import com.example.order.config.FeignConfig;
import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;
import java.util.stream.Collectors;

@FeignClient(name = "cart-service", url = "${cart.service.url}", configuration = FeignConfig.class)
public interface CartServiceClient {

    @GetMapping("/api/v1/carts/my-cart")
    CartApiResponse<CartResponse> getCartByUserIdRaw(@RequestHeader("Authorization") String authorizationHeader);

    @DeleteMapping("/api/v1/carts/my-cart")
    void clearCart(@RequestHeader("Authorization") String authorizationHeader);

    default CartDTO getCartByUserId(String authorizationHeader) {
        try {
            CartApiResponse<CartResponse> response = getCartByUserIdRaw(authorizationHeader);
            if (response == null) {
                throw new RuntimeException("Received null response from cart-service");
            }
            if (response.getResult() == null) {
                throw new RuntimeException("Cart response is null in cart-service response: " + response);
            }
            CartResponse cartResponse = response.getResult();
            List<CartItemDTO> items = cartResponse.getItems() != null
                ? cartResponse.getItems().stream()
                    .map(item -> new CartItemDTO(item.getProductId(), item.getQuantity()))
                    .collect(Collectors.toList())
                : List.of();
            return new CartDTO(cartResponse.getUserId(), items);
        } catch (FeignException e) {
            throw new RuntimeException("FeignException while calling cart-service: status=" + e.status() + ", message=" + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while fetching cart: " + e.getMessage(), e);
        }
    }

       // 🔥 API mới: xoá / trừ những item đã checkout
    @PostMapping("/api/v1/carts/checkout-remove")
    void removeItemsAfterCheckout(
            @RequestBody List<CheckoutCartItemRequest> items,
            @RequestHeader("Authorization") String authorizationHeader
    );
}