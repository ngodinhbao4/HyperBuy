// Package: com.example.cartservice.config (hoặc một package security riêng)
package com.example.cartservice.config;


import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;


import java.util.Map;
import java.util.function.Supplier;

public class UserIdBasedAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final String pathVariableName;

    public UserIdBasedAuthorizationManager(String pathVariableName) {
        this.pathVariableName = pathVariableName;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext object) {
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return new AuthorizationDecision(false); // Không xác thực, từ chối
        }

        // Lấy userId từ path variable của request
        Map<String, String> uriVariables = object.getVariables();
        String requestedUserId = uriVariables.get(this.pathVariableName);

        if (requestedUserId == null || requestedUserId.trim().isEmpty()) {
            // Không tìm thấy path variable hoặc rỗng, có thể là lỗi cấu hình endpoint hoặc request sai
            return new AuthorizationDecision(false);
        }

        // Lấy userId từ principal (JWT token)
        String authenticatedUserId = null;
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            authenticatedUserId = jwt.getSubject(); // Claim 'sub' thường chứa user ID
            // Hoặc một claim tùy chỉnh khác: jwt.getClaimAsString("user_id");
        } else {
            // Xử lý trường hợp principal không phải là Jwt nếu có cơ chế xác thực khác
            authenticatedUserId = authentication.getName();
        }

        if (authenticatedUserId == null) {
            return new AuthorizationDecision(false); // Không lấy được userId từ token
        }
        boolean granted = authenticatedUserId.equals(requestedUserId);
        return new AuthorizationDecision(granted);
    }
}