// Package: com.example.order.service (hoặc package service của OrderService)
package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("orderSecurityService") // Tên bean để tham chiếu trong @PreAuthorize
public class OrderSecurityService {

    private final OrderRepository orderRepository;

    public OrderSecurityService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Kiểm tra xem người dùng đã xác thực có quyền truy cập vào một đơn hàng cụ thể không.
     * Người dùng có quyền nếu họ là chủ sở hữu của đơn hàng.
     *
     * @param authentication Đối tượng Authentication của người dùng hiện tại.
     * @param orderId ID của đơn hàng cần kiểm tra.
     * @return true nếu người dùng có quyền, false nếu không.
     */
    public boolean canAccessOrder(Authentication authentication, Long orderId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String authenticatedUserId = getUserIdFromAuthentication(authentication);
        if (authenticatedUserId == null) {
            return false;
        }

        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if (orderOptional.isEmpty()) {
            return false; // Đơn hàng không tồn tại, không cần kiểm tra quyền nữa (hoặc có thể coi là false)
        }

        Order order = orderOptional.get();
        // So sánh userId trong đơn hàng với userId đã xác thực
        // Giả sử Order entity có trường userId kiểu String
        return authenticatedUserId.equals(order.getUserId());
    }

    /**
     * Kiểm tra xem người dùng đã xác thực có phải là userId được yêu cầu hoặc có vai trò ADMIN không.
     *
     * @param authentication Đối tượng Authentication của người dùng hiện tại.
     * @param requestedUserId ID người dùng được yêu cầu trong path variable.
     * @return true nếu người dùng có quyền, false nếu không.
     */
    public boolean isOwnerOrAdmin(Authentication authentication, String requestedUserId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Kiểm tra vai trò ADMIN
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ADMIN")); // Hoặc "ROLE_ADMIN" tùy cấu hình
        if (isAdmin) {
            return true;
        }

        // Kiểm tra có phải chủ sở hữu
        String authenticatedUserId = getUserIdFromAuthentication(authentication);
        return authenticatedUserId != null && authenticatedUserId.equals(requestedUserId);
    }


    private String getUserIdFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getSubject(); // Claim 'sub' thường chứa user ID
            // Hoặc một claim tùy chỉnh khác: return jwt.getClaimAsString("user_id");
        } else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        }
        // Xử lý các loại Principal khác nếu có
        return authentication.getName();
    }
}
