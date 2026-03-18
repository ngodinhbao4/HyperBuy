// Package: com.example.cartservice.config
package com.example.cartservice.config;

import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity // Kích hoạt Spring Security Web
@EnableMethodSecurity // Kích hoạt bảo mật ở cấp độ phương thức (ví dụ: @PreAuthorize)
public class SecurityConfig {

    // Đọc secret key từ application.properties (hoặc biến môi trường)
    // Giá trị này PHẢI GIỐNG HỆT với key được sử dụng bởi UserService để ký token
    @Value("${jwt.signerKey}")
    private String jwtSecretKeyString;

    private static final String[] PUBLIC_MATCHERS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            // Thêm các public endpoint khác nếu có (ví dụ: health check của actuator)
            "/actuator/**" // Cho phép truy cập tất cả actuator endpoints (điều chỉnh nếu cần)
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Vô hiệu hóa CSRF vì chúng ta đang sử dụng JWT (stateless)
            .csrf(AbstractHttpConfigurer::disable)

            // Cấu hình CORS (Cross-Origin Resource Sharing)
            // Sử dụng CorsFilter bean bên dưới hoặc cấu hình trực tiếp ở đây
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.addAllowedOriginPattern("*"); // Cho phép tất cả các origin (điều chỉnh cho production)
                configuration.addAllowedMethod("*");    // Cho phép tất cả các method
                configuration.addAllowedHeader("*");    // Cho phép tất cả các header
                configuration.setAllowCredentials(true);
                return configuration;
            }))

            // Cấu hình bảo vệ cho các HTTP request
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PUBLIC_MATCHERS).permitAll() // Cho phép truy cập công khai các PUBLIC_MATCHERS
                // Các endpoint cho giỏ hàng của người dùng hiện tại chỉ cần xác thực
                // vì userId sẽ được lấy từ token
                .requestMatchers("/api/v1/carts/init").authenticated()
                .requestMatchers("/api/v1/carts/my-cart/**").authenticated()
                // Nếu bạn có các endpoint quản trị yêu cầu {userId} trong path,
                // bạn có thể thêm chúng ở đây với role check, ví dụ:
                // .requestMatchers(HttpMethod.GET, "/api/v1/carts/admin/{userId}/**").hasRole("ADMIN")
                .anyRequest().authenticated() // Tất cả các request khác cần được xác thực
            )

            // Cấu hình OAuth2 Resource Server để xử lý JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder()) // Cung cấp JwtDecoder đã được cấu hình cho HS512
                    .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Tùy chỉnh cách chuyển đổi JWT thành Authentication
                )
                // (Tùy chọn) Thêm custom AuthenticationEntryPoint nếu muốn
                // .authenticationEntryPoint(new CustomCartAuthenticationEntryPoint())
            )

            // Cấu hình session management là STATELESS (không tạo session)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /**
     * Tạo bean JwtDecoder để giải mã và xác thực JWT.
     * Sử dụng NimbusJwtDecoder với Secret Key cho thuật toán HS512.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // Chuyển đổi chuỗi secret key (plain text UTF-8 bytes) thành SecretKey object
        // Nếu key của bạn được lưu trữ dưới dạng Base64 encoded trong properties,
        // bạn cần decode Base64 trước: byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtSecretKeyString);
        byte[] keyBytes = jwtSecretKeyString.getBytes();

        // Tên thuật toán trong SecretKeySpec phải khớp với thuật toán JWS được sử dụng (ví dụ: "HmacSHA512")
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA512");

        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512) // Chỉ định thuật toán HS512
                .build();
    }

    /**
     * Tùy chỉnh cách chuyển đổi JWT thành đối tượng Authentication của Spring Security.
     * Đặc biệt, cấu hình để trích xuất authorities (roles) từ JWT.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Cấu hình tên claim chứa authorities/roles trong JWT.
        // Dựa trên SecurityConfig của UserService, bạn đã setAuthorityPrefix("").
        // Nếu UserService không thêm prefix "ROLE_", thì ở đây cũng không cần.
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // Để trống nếu roles trong token không có prefix "ROLE_"
                                                            // Hoặc "ROLE_" nếu bạn muốn Spring tự thêm.
                                                            // Phải nhất quán với cách roles được cấp phát.

        // Nếu roles nằm trong một claim cụ thể, ví dụ "authorities" hoặc "roles" hoặc "realm_access.roles" (Keycloak)
        // grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // Thay "authorities" bằng tên claim thực tế

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        // Mặc định, principal name (userId) được lấy từ claim "sub" (subject) của JWT.
        // Nếu userId của bạn nằm trong một claim khác, bạn có thể cấu hình ở đây:
        // jwtConverter.setPrincipalClaimName("user_id"); // Ví dụ
        return jwtConverter;
    }

    /**
     * Bean cấu hình CORS một cách tập trung hơn (tùy chọn, có thể dùng cách ở trên trong http.cors(...))
     * Nếu bạn dùng bean này, hãy đảm bảo http.cors(Customizer.withDefaults()) hoặc tương tự được gọi.
     */
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
}
