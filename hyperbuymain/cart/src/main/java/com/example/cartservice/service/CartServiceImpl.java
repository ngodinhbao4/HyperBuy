    package com.example.cartservice.service;

    import com.example.cartservice.client.ProductServiceClient;
    import com.example.cartservice.dto.CheckoutCartItemRequest;
    import com.example.cartservice.dto.request.CartItemRequest;
    import com.example.cartservice.dto.response.CartResponse;
    import com.example.cartservice.dto.response.ApiResponse;
    import com.example.cartservice.dto.response.CartItemResponse;
    import com.example.cartservice.dto.request.ProductDetailRequest;
    import com.example.cartservice.entity.Cart;
    import com.example.cartservice.entity.CartItem;
    import com.example.cartservice.exception.CartException;
    import com.example.cartservice.exception.ErrorCodeCart;
    import com.example.cartservice.mapper.CartItemMapper;
    import com.example.cartservice.mapper.CartMapper;
    import com.example.cartservice.repository.CartItemRepository;
    import com.example.cartservice.repository.CartRepository;
    import feign.FeignException;
    import jakarta.annotation.PostConstruct;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.dao.DataAccessException;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.util.List;
    import java.util.Map;
    import java.util.Optional;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class CartServiceImpl implements CartService {

        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
        private final ProductServiceClient productServiceClient;
        private final CartMapper cartMapper;
        private final CartItemMapper cartItemMapper;
        @Value("${product.service.public.url:http://localhost:8081}")
        private String productServicePublicUrl;

        @PostConstruct
        public void init() {
            log.info("ProductService URL configured: {}", productServicePublicUrl);
        }

        private Cart getOrCreateCartEntityByUserId(String userId) {
            return cartRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        log.info("No cart found for user {}, creating a new one.", userId);
                        Cart newCart = new Cart(userId);
                        return cartRepository.save(newCart);
                    });
        }

        private ProductDetailRequest fetchProductDetailsOrFail(String productId, String token) {
        try {
            // Gọi product-service: nhận về ApiResponse<ProductDetailRequest>
            ApiResponse<ProductDetailRequest> response =
                    productServiceClient.getProductById(productId, token);

            ProductDetailRequest productDetail =
                    (response != null) ? response.getResult() : null;

            if (productDetail == null || productDetail.getId() == null) {
                throw new CartException(
                        ErrorCodeCart.PRODUCT_NOT_AVAILABLE,
                        "Product details are invalid for ID: " + productId
                );
            }

            // Giữ nguyên logic fix imageUrl (nếu bạn đang có biến productServicePublicUrl)
            if (productDetail.getImageUrl() != null
                    && productDetail.getImageUrl().contains("productservice:8081")) {

                String publicHost = productServicePublicUrl.replace("http://", "");
                String newImageUrl = productDetail.getImageUrl()
                        .replace("productservice:8081", publicHost);

                log.info("Adjusted imageUrl from {} to {}", productDetail.getImageUrl(), newImageUrl);
                productDetail.setImageUrl(newImageUrl);
            }

            return productDetail;

        } catch (FeignException e) {
            log.error("FeignException when calling ProductService: {}", e.getMessage(), e);
            throw new CartException(
                    ErrorCodeCart.PRODUCT_NOT_AVAILABLE,
                    "Could not retrieve product details for ID: " + productId
            );
        }
    }

        private CartResponse mapCartToEnrichedDto(Cart cart, String token) {
            CartResponse cartDto = cartMapper.toCartResponse(cart);
            if (cartDto.getItems() != null && !cartDto.getItems().isEmpty()) {
                List<CartItemResponse> enrichedItems = cart.getItems().stream()
                    .map(cartItemEntity -> {
                        ProductDetailRequest productDetail = fetchProductDetailsOrFail(cartItemEntity.getProductId(), token);
                        return cartItemMapper.toCartItemResponse(cartItemEntity, productDetail);
                    })
                    .collect(Collectors.toList());
                cartDto.setItems(enrichedItems);
            }
            return cartDto;
        }

        @Override
        @Transactional(readOnly = true)
        public CartResponse getCartByUserId(String userId, String token) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "User ID cannot be empty for fetching cart.");
            }
            log.info("Fetching cart for user ID: {}", userId);
            Cart cart = getOrCreateCartEntityByUserId(userId);
            try {
                // Sử dụng mapCartToEnrichedDto để làm giàu dữ liệu sản phẩm
                CartResponse cartResponse = mapCartToEnrichedDto(cart, token);
                log.info("Successfully fetched enriched cart for user {}: {}", userId, cartResponse);
                return cartResponse;
            } catch (CartException e) {
                log.error("CartException while mapping cart for user {}: {}", userId, e.getMessage(), e);
                CartResponse cartResponse = cartMapper.toCartResponse(cart);
                cartResponse.setItems(List.of()); // Trả về items rỗng nếu lỗi
                return cartResponse;
            } catch (Exception e) {
                log.error("Unexpected error while mapping cart for user {}: {}", userId, e.getMessage(), e);
                CartResponse cartResponse = cartMapper.toCartResponse(cart);
                cartResponse.setItems(List.of()); // Trả về items rỗng nếu lỗi
                return cartResponse;
            }
        }

        @Override
        @Transactional
        public CartResponse addItemToCart(String userId, CartItemRequest itemRequestDto, String token) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "User ID cannot be empty for adding item to cart.");
            }
            log.info("Adding item to cart for user ID: {}. Item: {}", userId, itemRequestDto);

            Cart cart = getOrCreateCartEntityByUserId(userId);
            ProductDetailRequest productDetail = fetchProductDetailsOrFail(itemRequestDto.getProductId(), token);

            int requestedQuantity = itemRequestDto.getQuantity();
            if (productDetail.getStockQuantity() == null || productDetail.getStockQuantity() < requestedQuantity) {
                throw new CartException(ErrorCodeCart.INSUFFICIENT_STOCK,
                        "Not enough stock for product '" + productDetail.getName() +
                        "'. Available: " + (productDetail.getStockQuantity() == null ? 0 : productDetail.getStockQuantity()) +
                        ", Requested: " + requestedQuantity);
            }

            Optional<CartItem> existingItemOpt = cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(itemRequestDto.getProductId()))
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                int newQuantity = existingItem.getQuantity() + requestedQuantity;
                if (productDetail.getStockQuantity() < newQuantity) {
                    throw new CartException(ErrorCodeCart.INSUFFICIENT_STOCK,
                            "Not enough stock to increase quantity for product '" + productDetail.getName() +
                            "'. Available: " + productDetail.getStockQuantity() + ", Requested total: " + newQuantity);
                }
                existingItem.setQuantity(newQuantity);
                cartItemRepository.save(existingItem);
                log.info("Updated quantity for product {} in cart {} to {}", itemRequestDto.getProductId(), cart.getId(), newQuantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProductId(itemRequestDto.getProductId());
                newItem.setQuantity(requestedQuantity);
                newItem.setPriceAtAddition(productDetail.getPrice());
                cart.addItem(newItem);
                log.info("Added new product {} to cart {}", itemRequestDto.getProductId(), cart.getId());
            }

            cartRepository.save(cart);
            return mapCartToEnrichedDto(cart, token);
        }

        @Override
        @Transactional
        public CartResponse updateCartItemQuantity(String userId, String productId, Integer newQuantity) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "User ID cannot be empty for updating item quantity.");
            }
            if (productId == null || productId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "Product ID cannot be empty for updating item quantity.");
            }
            if (newQuantity == null || newQuantity < 0) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "New quantity must be zero or positive.");
            }

            log.info("Updating quantity for product {} in cart of user {} to {}", productId, userId, newQuantity);

            if (newQuantity == 0) {
                return removeItemFromCart(userId, productId);
            }

            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new CartException(ErrorCodeCart.CART_NOT_FOUND));

            CartItem cartItem = cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new CartException(ErrorCodeCart.PRODUCT_NOT_FOUND_IN_CART, "Product " + productId + " not in cart."));

            // Lưu ý: Cần token để gọi fetchProductDetailsOrFail
            // Tạm thời bỏ qua kiểm tra stock vì không có token ở đây
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
            cartRepository.save(cart);
            log.info("Successfully updated quantity for product {} in cart {} to {}", productId, cart.getId(), newQuantity);
            return cartMapper.toCartResponse(cart); // Không làm giàu dữ liệu vì thiếu token
        }

        @Override
        @Transactional
        public CartResponse removeItemFromCart(String userId, String productId) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "User ID cannot be empty for removing item.");
            }
            if (productId == null || productId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "Product ID cannot be empty for removing item.");
            }
            log.info("Removing product {} from cart of user {}", productId, userId);

            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new CartException(ErrorCodeCart.CART_NOT_FOUND));

            CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartException(ErrorCodeCart.PRODUCT_NOT_FOUND_IN_CART, "Product " + productId + " not in cart to remove."));

            cart.getItems().remove(itemToRemove);
            Cart updatedCart = cartRepository.findById(cart.getId())
                    .orElseThrow(() -> new CartException(ErrorCodeCart.CART_NOT_FOUND));
            cartRepository.save(updatedCart);
            log.info("Successfully removed product {} from cart {}", productId, cart.getId());
            return cartMapper.toCartResponse(updatedCart); // Không làm giàu dữ liệu vì thiếu token
        }

        @Override
        @Transactional
        public void clearCart(String userId) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "User ID cannot be empty for clearing cart.");
            }
            log.info("Clearing cart for user ID: {}", userId);
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new CartException(ErrorCodeCart.CART_NOT_FOUND));

            if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                cartItemRepository.deleteAll(cart.getItems());
            }
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Successfully cleared cart for user ID: {}", userId);
        }

        @Override
        public String getOrCreateCartIdForUser(String userId) {
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("Attempted to get or create cart with null or empty userId.");
                throw new CartException(ErrorCodeCart.INVALID_CART_REQUEST, "User ID cannot be null or empty.");
            }

            log.info("Attempting to get or create cart for user ID: {}", userId);

            try {
                Optional<Cart> existingCartOpt = cartRepository.findByUserId(userId);

                if (existingCartOpt.isPresent()) {
                    Cart existingCart = existingCartOpt.get();
                    log.info("Cart found for user {}. Cart ID: {}", userId, existingCart.getId());
                    return existingCart.getId();
                } else {
                    log.info("No cart found for user {}. Creating a new cart.", userId);
                    Cart newCart = new Cart(userId);
                    Cart savedCart = cartRepository.save(newCart);
                    log.info("Successfully created and saved a new cart for user {}. New Cart ID: {}", userId, savedCart.getId());
                    return savedCart.getId();
                }
            } catch (DataAccessException dae) {
                log.error("Data access error while getting or creating cart for user {}: {}", userId, dae.getMessage(), dae);
                throw new CartException(ErrorCodeCart.UNCATEGORIZED_CART_EXCEPTION, "A database error occurred while processing the cart.");
            } catch (Exception e) {
                log.error("Unexpected error while getting or creating cart for user {}: {}", userId, e.getMessage(), e);
                throw new CartException(ErrorCodeCart.UNCATEGORIZED_CART_EXCEPTION, "An unexpected error occurred.");
            }
        }

        @Override
@Transactional
public void removeItemsAfterCheckout(String userId, List<CheckoutCartItemRequest> items) {
    if (items == null || items.isEmpty()) {
        return;
    }

    Cart cart = cartRepository.findByUserId(userId).orElse(null);
    if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
        return;
    }

    // productId trong CartItem là String → dùng Map<String, CartItem>
    Map<String, CartItem> cartItemMap = cart.getItems().stream()
            .collect(Collectors.toMap(
                    CartItem::getProductId,
                    ci -> ci
            ));

    for (CheckoutCartItemRequest req : items) {
        // Nếu productId trong CheckoutCartItemRequest là Long → đổi sang String
        String productIdKey = String.valueOf(req.getProductId());

        CartItem ci = cartItemMap.get(productIdKey);
        if (ci == null) {
            continue;
        }

        int newQty = ci.getQuantity() - req.getQuantity();
        if (newQty <= 0) {
            // xóa item khỏi cart
            cart.getItems().remove(ci);
            cartItemRepository.delete(ci);
        } else {
            ci.setQuantity(newQty);
        }
    }

    cartRepository.save(cart);
}
    }