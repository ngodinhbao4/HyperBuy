// Package: com.example.order.config (hoặc package config của OrderService)
package com.example.order.config;

import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Kích hoạt @PreAuthorize, @Secured
public class SecurityConfig {

    @Value("${jwt.signerKey}")
    private String jwtSecretKeyString;

    // Các endpoint công khai cho OrderService
    private static final String[] PUBLIC_MATCHERS_ORDER = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/actuator/**",
            "/api/v1/ **",
            "/api/v1/orders/**"
            // Thêm các public endpoint khác của OrderService nếu có
    };

    // Các endpoint người dùng có thể truy cập sau khi xác thực
    // Quyền truy cập chi tiết hơn (ví dụ: chỉ chủ sở hữu đơn hàng)
    // sẽ được xử lý ở cấp độ phương thức controller bằng @PreAuthorize
    private static final String API_ORDERS_BASE_PATH = "/api/v1/orders";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.addAllowedOriginPattern("*");
                configuration.addAllowedMethod("*");
                configuration.addAllowedHeader("*");
                configuration.setAllowCredentials(true);
                return configuration;
            }))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PUBLIC_MATCHERS_ORDER).permitAll()
                // Endpoint tạo đơn hàng: cần xác thực
                .requestMatchers(HttpMethod.POST, API_ORDERS_BASE_PATH).authenticated()
                // Endpoint lấy danh sách đơn hàng của người dùng hiện tại (ví dụ)
                .requestMatchers(HttpMethod.GET, API_ORDERS_BASE_PATH + "/my-orders").authenticated()
                // Endpoint lấy chi tiết đơn hàng theo orderId: cần xác thực.
                // Việc kiểm tra user có phải chủ sở hữu đơn hàng không sẽ dùng @PreAuthorize ở controller.
                .requestMatchers(HttpMethod.GET, API_ORDERS_BASE_PATH + "/{orderId}").authenticated()
                // Endpoint cập nhật trạng thái đơn hàng: cần xác thực.
                // Việc kiểm tra quyền (ví dụ: chỉ ADMIN hoặc hệ thống nội bộ) sẽ dùng @PreAuthorize.
                .requestMatchers(HttpMethod.PUT, API_ORDERS_BASE_PATH + "/{orderId}/status").authenticated()
                // Endpoint lấy danh sách đơn hàng theo userId (ví dụ cho admin hoặc user xem của chính mình)
                // Nếu cho admin: .requestMatchers(HttpMethod.GET, API_ORDERS_BASE_PATH + "/user/{userId}").hasRole("ADMIN")
                // Nếu cho user xem của chính mình, sẽ dùng @PreAuthorize ở controller để so sánh userId trong path với userId trong token.
                .requestMatchers(HttpMethod.GET, API_ORDERS_BASE_PATH + "/user/{userId}").authenticated()
                .requestMatchers("/api/v1/internal/orders/has-purchased").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = jwtSecretKeyString.getBytes();
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // Giữ nguyên như CartService
        // grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // Nếu roles nằm trong claim "authorities"

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        // jwtConverter.setPrincipalClaimName("user_id"); // Nếu userId nằm trong claim "user_id" thay vì "sub"
        return jwtConverter;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");        // ⭐ Cho phép x-store-id
        configuration.addExposedHeader("*");        // ⭐ FE có thể đọc response headers
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
