package com.example.order.service.iml;

import com.example.order.dto.AddressDTO;
import com.example.order.dto.CartDTO;
import com.example.order.dto.CartItemDTO;
import com.example.order.dto.OrderEvent;
import com.example.order.dto.ProductDTO;
import com.example.order.dto.request.CreateOrderRequest;
import com.example.order.dto.response.ApiResponRequest;
import com.example.order.dto.response.OrderItemResponse;
import com.example.order.dto.response.OrderResponse;
import com.example.order.dto.response.UserResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.ErrorCodeOrder;
import com.example.order.exception.OrderException;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import com.example.order.service.client.CartServiceClient;
import com.example.order.service.client.ProductServiceClient;
import com.example.order.service.client.UpdateStockRequest;
import com.example.order.service.client.UserServiceClient;
import com.example.order.service.client.VoucherServiceClient;
import com.example.order.service.client.CheckoutCartItemRequest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;        // ✅ thêm
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final CartServiceClient cartServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceClient userServiceClient;
    private final VoucherServiceClient voucherServiceClient;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest, String authorizationHeader) {
        logger.info("Attempting to create order for user ID: {}, Authorization: {}",
                createOrderRequest.getUserId(), authorizationHeader);

        // 1) Lấy username & UUID từ user-service
        String username = createOrderRequest.getUserId(); // ví dụ: admin
        logger.debug("Fetching user for username: {}", username);
        ApiResponRequest<UserResponse> userRes;
        try {
            userRes = userServiceClient.getUserByUsername(username, authorizationHeader);
            if (userRes.getResult() == null
                    || userRes.getResult().getId() == null
                    || userRes.getResult().getUsername() == null) {
                logger.error("UserResponse, id, or username is null for username: {}", username);
                throw new OrderException(
                        ErrorCodeOrder.USER_NOT_FOUND,
                        "User not found for username: " + username
                );
            }
        } catch (FeignException e) {
            logger.error("Failed to fetch user for username: {}. Error: {}", username, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.USER_NOT_FOUND,
                    "User not found for username: " + username,
                    e
            );
        }

        String userIdForOrder = userRes.getResult().getUsername(); // lưu username
        String userIdForNotification = userRes.getResult().getId(); // UUID (dùng cho voucher + noti)
        logger.debug("Retrieved username: {} and UUID: {} for username: {}",
                userIdForOrder, userIdForNotification, username);

        // 2) Lấy giỏ hàng từ cart-service
        // 2) Lấy giỏ hàng từ cart-service
        CartDTO cart = fetchCartFromService(userIdForNotification, authorizationHeader);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            logger.warn("Cart is empty for user ID: {}. Cannot create order.", userIdForNotification);
            throw new OrderException(ErrorCodeOrder.EMPTY_CART_FOR_ORDER);
        }
        List<CartItemDTO> cartItems = cart.getItems();
        logger.info("Cart fetched for user {}: {} items", userIdForNotification, cartItems.size());

        // 2.1) Xác định danh sách item sẽ thanh toán
List<CartItemDTO> itemsToOrder;

if (createOrderRequest.getItems() == null || createOrderRequest.getItems().isEmpty()) {
    // 🟢 Không gửi items trong request -> thanh toán toàn bộ giỏ
    itemsToOrder = cartItems;
    logger.info("No items specified in request. Will checkout ALL cart items.");
} else {
    // 🟡 Gửi danh sách items -> chỉ checkout các item này
    logger.info("Request contains {} items to checkout (partial checkout).", createOrderRequest.getItems().size());

    // Map cart theo productId để tra nhanh
    Map<Long, CartItemDTO> cartItemMap = new HashMap<>();
    for (CartItemDTO ci : cartItems) {
        cartItemMap.put(ci.getProductId(), ci);
    }

    itemsToOrder = createOrderRequest.getItems().stream().map(reqItem -> {
        CartItemDTO inCart = cartItemMap.get(reqItem.getProductId());
        if (inCart == null) {
            throw new OrderException(
                    ErrorCodeOrder.INVALID_ORDER_REQUEST,
                    "Product " + reqItem.getProductId() + " is not in cart."
            );
        }
        int requestedQty = reqItem.getQuantity();
        if (requestedQty <= 0) {
            throw new OrderException(
                    ErrorCodeOrder.INVALID_ORDER_REQUEST,
                    "Requested quantity for product " + reqItem.getProductId() + " must be > 0."
            );
        }

        int quantityToBuy = Math.min(requestedQty, inCart.getQuantity());

        CartItemDTO clone = new CartItemDTO();
        clone.setProductId(inCart.getProductId());
        clone.setQuantity(quantityToBuy);
        // nếu CartItemDTO có thêm field khác thì có thể copy thêm ở đây
        return clone;
    }).collect(Collectors.toList());

    if (itemsToOrder.isEmpty()) {
        throw new OrderException(
                ErrorCodeOrder.INVALID_ORDER_REQUEST,
                "No valid cart items selected for checkout."
        );
    }
}

logger.info("Will create order with {} items.", itemsToOrder.size());

        // 3) Kiểm tra từng sản phẩm (chỉ trên selectedItems)
        for (CartItemDTO item : itemsToOrder) {
    logger.info("Processing cart item: productId={}, quantity={}", item.getProductId(), item.getQuantity());
    ProductDTO product = fetchProductFromService(item.getProductId(), authorizationHeader);
    validateProductAvailability(product, item.getQuantity());
}

        // 4) Khởi tạo Order
        Order order = initializeOrder(createOrderRequest);  
        order.setUserId(userIdForOrder); // lưu username

        // 5) Tạo OrderItem từ selectedItems
        List<OrderItem> orderItems = itemsToOrder.stream()
        .map(item -> {
            ProductDTO product = fetchProductFromService(item.getProductId(), authorizationHeader);
            return createOrderItem(product, item.getQuantity(), order);
        })
        .collect(Collectors.toList());
order.setItems(orderItems);

        // 6) Tính tổng trước giảm
        BigDecimal totalAmountBeforeDiscount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmountBeforeDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException(
                    ErrorCodeOrder.INVALID_ORDER_REQUEST,
                    "Total amount must be greater than zero."
            );
        }

        // 7) Áp dụng voucher (nếu có)
        String voucherCode = createOrderRequest.getVoucherCode();
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                discountAmount = voucherServiceClient.applyVoucher(
                        userIdForNotification,   // UUID dùng cho user_voucher.user_id
                        voucherCode,
                        totalAmountBeforeDiscount,
                        authorizationHeader
                );
            } catch (Exception e) {
                logger.error("Failed to apply voucher {} for user {}: {}",
                        voucherCode, userIdForNotification, e.getMessage(), e);
                throw new OrderException(
                        ErrorCodeOrder.INVALID_ORDER_REQUEST,
                        "Cannot apply voucher: " + voucherCode,
                        e
                );
            }
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        // 8) Tính tổng sau giảm (không âm)
        BigDecimal finalAmount = totalAmountBeforeDiscount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 9) Gán vào Order
        order.setTotalAmount(totalAmountBeforeDiscount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setVoucherCode(
                (voucherCode != null && !voucherCode.isBlank()) ? voucherCode : null
        );

        logger.info("Order discount for user {}: total={}, discount={}, final={}, voucher={}",
                userIdForOrder, totalAmountBeforeDiscount, discountAmount, finalAmount, voucherCode);

        // 10) Lưu đơn
Order savedOrder = saveOrderToDatabase(order);

// 11) Trừ tồn kho
decreaseProductStock(savedOrder, authorizationHeader);

// 12) Cập nhật giỏ hàng sau thanh toán
// - Nếu checkout ALL giỏ (request không gửi items) -> clearCart
// - Nếu checkout 1 phần -> chỉ trừ số lượng đã mua
if (createOrderRequest.getItems() == null || createOrderRequest.getItems().isEmpty()) {
    // Thanh toán toàn bộ
    clearUserCart(userIdForNotification, savedOrder.getId(), authorizationHeader);
} else {
    // Thanh toán một phần
    adjustCartAfterCheckout(userIdForNotification, itemsToOrder, savedOrder.getId(), authorizationHeader);
}

        // 13) Xác nhận đơn
        confirmOrder(savedOrder);

        // 14) Nếu có voucher & đã giảm > 0 → đánh dấu đã dùng
        if (savedOrder.getVoucherCode() != null
                && savedOrder.getDiscountAmount() != null
                && savedOrder.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                voucherServiceClient.markVoucherUsed(
                        userIdForNotification,
                        savedOrder.getVoucherCode(),
                        authorizationHeader
                );
            } catch (Exception e) {
                logger.error("Failed to mark voucher {} as used for user {}: {}",
                        savedOrder.getVoucherCode(), userIdForNotification, e.getMessage(), e);
                // không throw nữa để đơn hàng vẫn thành công
            }
        }

        // 15) Gửi event RabbitMQ
        OrderEvent event = new OrderEvent();
        event.setId(savedOrder.getId());
        event.setUserId(userIdForNotification);
        event.setStatus(savedOrder.getStatus().name());
        event.setAuthorizationHeader(authorizationHeader);
        logger.debug("Sending OrderEvent for order creation: {}", event);
        rabbitTemplate.convertAndSend("order_notifications", event);

        return mapOrderToResponseDTO(savedOrder);
    }

    private List<CartItemDTO> resolveSelectedItems(CreateOrderRequest request, List<CartItemDTO> cartItems) {
        List<CartItemDTO> fromRequest = request.getItems();
        if (fromRequest == null || fromRequest.isEmpty()) {
            // Thanh toán toàn bộ giỏ hàng
            return cartItems;
        }

        // map productId -> CartItemDTO trong giỏ
        Map<Long, CartItemDTO> cartItemMap = cartItems.stream()
                .collect(Collectors.toMap(CartItemDTO::getProductId, c -> c));

        return fromRequest.stream().map(reqItem -> {
            CartItemDTO cartItem = cartItemMap.get(reqItem.getProductId());
            if (cartItem == null) {
                throw new OrderException(
                        ErrorCodeOrder.INVALID_ORDER_REQUEST,
                        "Product " + reqItem.getProductId() + " is not in cart."
                );
            }

            int quantityToOrder = reqItem.getQuantity();
            if (quantityToOrder <= 0) {
                throw new OrderException(
                        ErrorCodeOrder.INVALID_ORDER_REQUEST,
                        "Invalid quantity for product " + reqItem.getProductId()
                );
            }

            if (quantityToOrder > cartItem.getQuantity()) {
                // Không cho mua nhiều hơn số lượng trong giỏ
                quantityToOrder = cartItem.getQuantity();
            }

            cartItem.setQuantity(quantityToOrder);
            return cartItem;
        }).collect(Collectors.toList());
    }

    private CartDTO fetchCartFromService(String userId, String authorizationHeader) {
        logger.info("Fetching cart for user {}", userId);
        try {
            // hiện tại CartServiceImpl bên bạn đang dùng token để lấy current user
            return cartServiceClient.getCartByUserId(authorizationHeader);
        } catch (Exception e) {
            logger.error("Failed to fetch cart for user {}: {}", userId, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.CART_SERVICE_UNREACHABLE,
                    "Could not fetch cart for user " + userId,
                    e
            );
        }
    }

    private ProductDTO fetchProductFromService(Long productId, String authorizationHeader) {
        try {
            ApiResponRequest<ProductDTO> response =
                    productServiceClient.getProductById(productId, authorizationHeader);

            if (response == null || response.getResult() == null) {
                throw new OrderException(
                        ErrorCodeOrder.PRODUCT_NOT_FOUND_FOR_ORDER,
                        "Product with ID " + productId + " not found."
                );
            }

            ProductDTO product = response.getResult();
            logger.info("Fetched product from product-service: id={}, name={}, active={}, stock={}",
                    product.getId(), product.getName(), product.isActive(), product.getStockQuantity());

            return product;
        } catch (FeignException e) {
            logger.error("FeignException fetching product {}: {}", productId, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.PRODUCT_SERVICE_UNREACHABLE_FOR_ORDER,
                    "Could not fetch product " + productId,
                    e
            );
        }
    }

    private void validateProductAvailability(ProductDTO product, int requestedQuantity) {
        if (!product.isActive() || product.isDeleted()) {
            throw new OrderException(
                    ErrorCodeOrder.PRODUCT_UNAVAILABLE_FOR_ORDER,
                    "Product " + product.getId() + " is unavailable."
            );
        }
        if (product.getStockQuantity() < requestedQuantity) {
            throw new OrderException(
                    ErrorCodeOrder.INSUFFICIENT_STOCK_FOR_ORDER,
                    "Insufficient stock for product " + product.getId()
            );
        }
    }

    // ✅ Đơn giản hoá: billingAddress null → dùng shippingAddress
    private Order initializeOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        AddressDTO shipping = request.getShippingAddress();
        AddressDTO billing = request.getBillingAddress() != null
                ? request.getBillingAddress()
                : shipping; // nếu không gửi billing → dùng shipping luôn

        mapAddressDtoToOrder(shipping, order, true);
        mapAddressDtoToOrder(billing, order, false);

        order.setPaymentMethod(request.getPaymentMethod());
        return order;
    }

    private OrderItem createOrderItem(ProductDTO product, int quantity, Order order) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(product.getId());
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getPrice());
        orderItem.setStoreId(product.getSellerInfo().getStoreId());  // ✔ đúng JSON

        

        String imageUrl = product.getImageUrl();
        if (imageUrl != null && imageUrl.contains("productservice:8081")) {
            imageUrl = imageUrl.replace("productservice:8081", "localhost:8081");
        }
        orderItem.setImageUrl(imageUrl);
        orderItem.setProductName(product.getName());
        orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return orderItem;
    }

    private Order saveOrderToDatabase(Order order) {
        logger.info("Saving order for user: {}", order.getUserId());
        try {
            return orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Failed to save order: {}", e.getMessage(), e);
            throw new OrderException(ErrorCodeOrder.DATABASE_ERROR, "Could not save order.");
        }
    }

    private void decreaseProductStock(Order savedOrder, String authorizationHeader) {
        try {
            for (OrderItem item : savedOrder.getItems()) {
                UpdateStockRequest request = new UpdateStockRequest(-item.getQuantity());
                productServiceClient.decreaseStock(item.getProductId(), request, authorizationHeader);
                logger.debug("Decreased stock for product {} by {}", item.getProductId(), item.getQuantity());
            }
        } catch (FeignException e) {
            logger.error("FeignException decreasing stock for order {}: {}", savedOrder.getId(), e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.PRODUCT_SERVICE_STOCK_UPDATE_FAILED,
                    "Failed to update stock.",
                    e
            );
        }
    }

    private void clearUserCart(String userId, Long orderId, String authorizationHeader) {
        try {
            cartServiceClient.clearCart(authorizationHeader);
            logger.info("Cleared cart for user {} after order {}", userId, orderId);
        } catch (Exception e) {
            logger.error("Failed to clear cart for user {} after order {}: {}", userId, orderId, e.getMessage(), e);
        }
    }

    private void confirmOrder(Order order) {
        order.setStatus(OrderStatus.PENDING);
        try {
            orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Failed to confirm order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        logger.debug("Fetching order by ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        ErrorCodeOrder.ORDER_NOT_FOUND,
                        "Order not found with ID: " + orderId
                ));
        return mapOrderToResponseDTO(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        logger.debug("Fetching orders for user ID: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapOrderToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus, String authorizationHeader) {
        logger.info("Attempting to update status for order ID: {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        ErrorCodeOrder.ORDER_NOT_FOUND,
                        "Order not found with ID: " + orderId
                ));

        order.setStatus(newStatus);
        Order updatedOrder;
        try {
            updatedOrder = orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Failed to update status for order {}: {}", orderId, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.ORDER_UPDATE_FAILED,
                    "Could not save updated order status.",
                    e
            );
        }
        logger.info("Order ID {} status updated to {}.", updatedOrder.getId(), updatedOrder.getStatus());

        // lấy UUID từ user-service để gửi noti
        String username = updatedOrder.getUserId();
        logger.debug("Fetching user for username: {}", username);
        ApiResponRequest<UserResponse> response;
        try {
            response = userServiceClient.getUserByUsername(username, authorizationHeader);
            if (response.getResult() == null || response.getResult().getId() == null) {
                logger.error("UserResponse or id is null for username: {}", username);
                throw new OrderException(
                        ErrorCodeOrder.USER_NOT_FOUND,
                        "User ID is null for username: " + username
                );
            }
        } catch (FeignException e) {
            logger.error("Failed to fetch user for username: {}. Error: {}", username, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.USER_NOT_FOUND,
                    "User not found for username: " + username,
                    e
            );
        }
        String userIdForNotification = response.getResult().getId();

        // Gửi event
        OrderEvent event = new OrderEvent();
        event.setId(updatedOrder.getId());
        event.setUserId(userIdForNotification);
        event.setStatus(updatedOrder.getStatus().name());
        event.setAuthorizationHeader(authorizationHeader);
        logger.debug("Sending OrderEvent: {}", event);
        rabbitTemplate.convertAndSend("order_notifications", event);

        if (newStatus == OrderStatus.CANCELLED) {
            handleOrderCancellationStockAdjustment(updatedOrder, authorizationHeader);
        }
        return mapOrderToResponseDTO(updatedOrder);
    }

    private void handleOrderCancellationStockAdjustment(Order cancelledOrder, String authorizationHeader) {
        logger.info("Order {} cancelled. Increasing stock.", cancelledOrder.getId());
        try {
            for (OrderItem item : cancelledOrder.getItems()) {
                UpdateStockRequest request = new UpdateStockRequest(item.getQuantity());
                productServiceClient.increaseStock(item.getProductId(), request, authorizationHeader);
                logger.debug("Increased stock for product {} by {}", item.getProductId(), item.getQuantity());
            }
        } catch (Exception e) {
            logger.error("Failed to increase stock for order {}: {}", cancelledOrder.getId(), e.getMessage(), e);
        }
    }

    private OrderResponse mapOrderToResponseDTO(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus());

        dto.setTotalAmount(order.getTotalAmount());
        dto.setVoucherCode(order.getVoucherCode());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setFinalAmount(order.getFinalAmount());

        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentTransactionId(order.getPaymentTransactionId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippingAddress(mapOrderToAddressDTO(order, true));
        dto.setBillingAddress(mapOrderToAddressDTO(order, false));

        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream()
                    .map(this::mapOrderItemToResponse)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private OrderItemResponse mapOrderItemToResponse(OrderItem item) {
        OrderItemResponse dto = new OrderItemResponse();
        dto.setProductId(item.getProductId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && imageUrl.contains("productservice:8081")) {
            imageUrl = imageUrl.replace("productservice:8081", "localhost:8081");
        }
        dto.setImageUrl(imageUrl);
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private void mapAddressDtoToOrder(AddressDTO addressDTO, Order order, boolean isShipping) {
        if (addressDTO == null) return;
        if (isShipping) {
            order.setShippingAddressLine1(addressDTO.getAddressLine1());
            order.setShippingAddressLine2(addressDTO.getAddressLine2());
            order.setShippingCity(addressDTO.getCity());
            order.setShippingPostalCode(addressDTO.getPostalCode());
            order.setShippingCountry(addressDTO.getCountry());
        } else {
            order.setBillingAddressLine1(addressDTO.getAddressLine1());
            order.setBillingAddressLine2(addressDTO.getAddressLine2());
            order.setBillingCity(addressDTO.getCity());
            order.setBillingPostalCode(addressDTO.getPostalCode());
            order.setBillingCountry(addressDTO.getCountry());
        }
    }

    private AddressDTO mapOrderToAddressDTO(Order order, boolean isShipping) {
        AddressDTO dto = new AddressDTO();
        if (isShipping) {
            dto.setAddressLine1(order.getShippingAddressLine1());
            dto.setAddressLine2(order.getShippingAddressLine2());
            dto.setCity(order.getShippingCity());
            dto.setPostalCode(order.getShippingPostalCode());
            dto.setCountry(order.getShippingCountry());
        } else {
            dto.setAddressLine1(order.getBillingAddressLine1());
            dto.setAddressLine2(order.getBillingAddressLine2());
            dto.setCity(order.getBillingCity());
            dto.setPostalCode(order.getBillingPostalCode());
            dto.setCountry(order.getBillingCountry());
        }
        if (dto.getAddressLine1() == null && dto.getAddressLine2() == null &&
                dto.getCity() == null && dto.getPostalCode() == null && dto.getCountry() == null) {
            return null;
        }
        return dto;
    }

        private void adjustCartAfterCheckout(
            String userId,
            List<CartItemDTO> purchasedItems,
            Long orderId,
            String authorizationHeader
    ) {
        try {
            List<CheckoutCartItemRequest> payload = purchasedItems.stream()
                    .map(i -> new CheckoutCartItemRequest(i.getProductId(), i.getQuantity()))
                    .collect(Collectors.toList());

            cartServiceClient.removeItemsAfterCheckout(payload, authorizationHeader);
            logger.info("Adjusted cart for user {} after order {} ({} items).",
                    userId, orderId, purchasedItems.size());
        } catch (Exception e) {
            logger.error("Failed to adjust cart for user {} after order {}: {}",
                    userId, orderId, e.getMessage(), e);
            // không throw để đơn vẫn thành công
        }
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatusBySeller(Long orderId, OrderStatus newStatus, String authorizationHeader) {

        logger.info("Seller is attempting to update status for order ID: {} to {}", orderId, newStatus);

        // 1) Lấy order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        ErrorCodeOrder.ORDER_NOT_FOUND,
                        "Order not found with ID: " + orderId
                ));

        // 2) Lấy username seller từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new OrderException(
                    ErrorCodeOrder.USER_NOT_FOUND,
                    "Cannot determine current seller from security context."
            );
        }
        String sellerUsername = auth.getName();   // ví dụ: "Kid"
        logger.debug("Current seller username from SecurityContext: {}", sellerUsername);

        // 3) Gọi user-service lấy thông tin seller + storeId
        ApiResponRequest<UserResponse> sellerRes;
        try {
            sellerRes = userServiceClient.getUserByUsername(sellerUsername, authorizationHeader);

            if (sellerRes == null || sellerRes.getResult() == null) {
                logger.error("Seller user is null for username: {}", sellerUsername);
                throw new OrderException(
                        ErrorCodeOrder.USER_NOT_FOUND,
                        "Seller user not found for username: " + sellerUsername
                );
            }

            UserResponse seller = sellerRes.getResult();

            if (seller.getStore() == null || seller.getStore().getStoreId() == null) {
                logger.error("Seller user or storeId is null for username: {}", sellerUsername);
                throw new OrderException(
                        ErrorCodeOrder.STORE_ID_NULL,
                        "Store ID is null for seller username: " + sellerUsername
                );
            }

            String sellerStoreId = seller.getStore().getStoreId();
            logger.debug("Seller storeId = {} for seller username {}", sellerStoreId, sellerUsername);

            // 4) Kiểm tra order có chứa item thuộc store này không
            boolean hasItemOfSeller = order.getItems().stream()
                    .anyMatch(i -> sellerStoreId.equals(i.getStoreId()));

            if (!hasItemOfSeller) {
                logger.warn("Seller {} (storeId = {}) tried to update order {} that does not belong to them.",
                        sellerUsername, sellerStoreId, orderId);
                throw new OrderException(
                        ErrorCodeOrder.FORBIDDEN_SELLER_ORDER,
                        "You are not allowed to update this order."
                );
            }

        } catch (FeignException e) {
            logger.error("Failed to fetch seller info for username: {}. Error: {}", sellerUsername, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.USER_SERVICE_UNREACHABLE,
                    "Could not fetch seller info for username: " + sellerUsername,
                    e
            );
        }

        // 5) Kiểm tra trạng thái hợp lệ
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderException(
                    ErrorCodeOrder.INVALID_STATUS_UPDATE,
                    "Cannot change status of a cancelled or delivered order."
            );
        }

        // 6) Cập nhật trạng thái
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order updatedOrder;
        try {
            updatedOrder = orderRepository.save(order);
        } catch (Exception e) {
            logger.error("Failed to update status for order {}: {}", orderId, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.ORDER_UPDATE_FAILED,
                    "Could not save updated order status.",
                    e
            );
        }

        logger.info("Order ID {} status updated to {} by seller {}.",
                updatedOrder.getId(), updatedOrder.getStatus(), sellerUsername);

        // 7) Gửi event RabbitMQ cho BUYER (customer) – dùng UUID
        try {
            // updatedOrder.getUserId() đang là username của khách (vd: "hbao")
            String buyerUsername = updatedOrder.getUserId();
            logger.debug("Fetching buyer info for username: {}", buyerUsername);

            ApiResponRequest<UserResponse> buyerRes =
                    userServiceClient.getUserByUsername(buyerUsername, authorizationHeader);

            if (buyerRes == null || buyerRes.getResult() == null || buyerRes.getResult().getId() == null) {
                logger.error("Buyer not found or id is null for username: {}", buyerUsername);
                throw new OrderException(
                        ErrorCodeOrder.USER_NOT_FOUND,
                        "Buyer not found for username: " + buyerUsername
                );
            }

            String buyerIdForNotification = buyerRes.getResult().getId();  // UUID

            OrderEvent event = new OrderEvent();
            event.setId(updatedOrder.getId());
            event.setUserId(buyerIdForNotification);   // ✅ Gửi UUID giống các chỗ khác
            event.setStatus(updatedOrder.getStatus().name());
            event.setAuthorizationHeader(authorizationHeader);
            logger.debug("Sending OrderEvent for seller update: {}", event);
            rabbitTemplate.convertAndSend("order_notifications", event);

        } catch (FeignException e) {
            logger.error("Failed to fetch buyer info for username from order: {}. Error: {}",
                    updatedOrder.getUserId(), e.getMessage(), e);
            // Có thể throw hoặc chỉ log, tuỳ bạn muốn noti fail có làm fail API hay không
        }


        // 8) Nếu seller CANCELLED order → hoàn stock
        if (newStatus == OrderStatus.CANCELLED) {
            handleOrderCancellationStockAdjustment(updatedOrder, authorizationHeader);
        }

        // 9) Trả response
        return mapOrderToResponseDTO(updatedOrder);
    }

    private String getCurrentSellerStoreId(String authorizationHeader) {
        // Lấy username từ SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new OrderException(
                    ErrorCodeOrder.USER_NOT_FOUND,
                    "Cannot determine current seller from security context."
            );
        }
        String sellerUsername = auth.getName();
        logger.debug("Current seller username from SecurityContext: {}", sellerUsername);

        // Gọi user-service lấy thông tin seller
        ApiResponRequest<UserResponse> sellerRes;
        try {
            sellerRes = userServiceClient.getUserByUsername(sellerUsername, authorizationHeader);
        } catch (FeignException e) {
            logger.error("Failed to fetch seller info for username: {}. Error: {}",
                    sellerUsername, e.getMessage(), e);
            throw new OrderException(
                    ErrorCodeOrder.USER_SERVICE_UNREACHABLE,
                    "Could not fetch seller info for username: " + sellerUsername,
                    e
            );
        }

        if (sellerRes == null || sellerRes.getResult() == null) {
            logger.error("Seller not found from user-service for username: {}", sellerUsername);
            throw new OrderException(
                    ErrorCodeOrder.USER_NOT_FOUND,
                    "Seller not found for username: " + sellerUsername
            );
        }

        UserResponse seller = sellerRes.getResult();
        if (seller.getStore() == null || seller.getStore().getStoreId() == null) {
            logger.error("Seller storeId is null for username: {}", sellerUsername);
            throw new OrderException(
                    ErrorCodeOrder.STORE_ID_NULL,
                    "Store ID is null for seller username: " + sellerUsername
            );
        }

        String storeId = seller.getStore().getStoreId();
        logger.debug("Seller storeId = {} for seller username {}", storeId, sellerUsername);
        return storeId;
    }

        @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForSellerByStatus(OrderStatus status, String authorizationHeader) {
        String sellerStoreId = getCurrentSellerStoreId(authorizationHeader);

        logger.info("Seller with storeId {} is loading orders with status {}", sellerStoreId, status);

        List<Order> orders = orderRepository.findByStoreIdAndStatus(sellerStoreId, status);

        return orders.stream()
                .map(this::mapOrderToResponseDTO)
                .collect(Collectors.toList());
    }

        @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderForSellerById(Long orderId, String authorizationHeader) {
        String sellerStoreId = getCurrentSellerStoreId(authorizationHeader);

        logger.info("Seller with storeId {} is requesting order detail for id {}", sellerStoreId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        ErrorCodeOrder.ORDER_NOT_FOUND,
                        "Order not found with ID: " + orderId
                ));

        // Đảm bảo đơn này có ít nhất 1 item thuộc store của seller
        boolean hasItemOfSeller = order.getItems().stream()
                .anyMatch(i -> sellerStoreId.equals(i.getStoreId()));

        if (!hasItemOfSeller) {
            logger.warn("Order {} does not contain items for storeId {}. Access denied.",
                    orderId, sellerStoreId);

            // Có thể dùng ERROR khác, nhưng để chắc chắn không lỗi enum thì dùng lại ORDER_NOT_FOUND
            throw new OrderException(
                    ErrorCodeOrder.ORDER_NOT_FOUND,
                    "Order not found for current seller store."
            );
        }

        return mapOrderToResponseDTO(order);
    }

}
