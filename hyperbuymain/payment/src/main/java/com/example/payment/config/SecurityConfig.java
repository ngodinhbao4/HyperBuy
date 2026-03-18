package com.example.payment.config;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Value("${jwt.signerKey}")
    private String jwtSecretKeyString;

    private static final String API_PAYMENTS_BASE_PATH = "/api/v1/payments";

    // Các endpoint công khai (ví dụ: webhook từ cổng thanh toán nếu nó không dùng xác thực phức tạp)
    // Cần cẩn thận với webhook, nên có cơ chế xác thực riêng (ví dụ: signature header)
    private static final String[] PUBLIC_MATCHERS_PAYMENT = {
            API_PAYMENTS_BASE_PATH + "/webhook/**", // Cần bảo vệ webhook cẩn thận!
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/actuator/**",
            "/api/v1/ **",
            "/api/v1/payments/ **"

    };

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
                .requestMatchers(PUBLIC_MATCHERS_PAYMENT).permitAll()
                // Các endpoint xử lý thanh toán, lấy thông tin thanh toán thường được gọi từ service khác (OrderService)
                // và nên được bảo vệ. OrderService khi gọi PaymentService sẽ cần gửi token (nếu PaymentService yêu cầu)
                // hoặc sử dụng một cơ chế xác thực service-to-service khác (ví dụ: mTLS, API key).
                // Giả sử các endpoint này yêu cầu xác thực từ OrderService (hoặc người dùng nếu gọi trực tiếp)
                .requestMatchers(HttpMethod.POST, API_PAYMENTS_BASE_PATH + "/process").authenticated()
                .requestMatchers(HttpMethod.GET, API_PAYMENTS_BASE_PATH + "/order/{orderId}").authenticated()
                .requestMatchers(HttpMethod.GET, API_PAYMENTS_BASE_PATH + "/{paymentId}").authenticated()
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
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtConverter;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
