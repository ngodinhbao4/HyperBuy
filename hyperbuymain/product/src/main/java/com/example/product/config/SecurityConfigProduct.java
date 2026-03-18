package com.example.product.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfigProduct {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigProduct.class);

    // Pattern phục vụ ảnh: /product-images/**
    @Value("${app.static-resource.public-path-pattern:/files/**}")
    private String publicImagesPathPattern;

    // 🔑 Dùng đúng key trong application.properties:
    // jwt.signerKey=!TJXchW5FLOeSBb63Kck+DFHTaRpWL4JUGcWFgWxUG5S1F/ly/LgJxHnMQaF46A/i
    @Value("${jwt.signerKey}")
    private String jwtSignerKey;

    @PostConstruct
    public void init() {
        logger.info("Giá trị publicImagesPathPattern trong SecurityConfigProduct: '{}'", publicImagesPathPattern);
    }

    // ✅ Bean JwtDecoder – HS512, khớp với token của bạn (alg=HS512)
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSignerKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512" // tương ứng HS512
        );

        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChainProduct(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Ảnh public không cần token
                        .requestMatchers(publicImagesPathPattern).permitAll()
                        .requestMatchers("/api/v1/recommendations/guest").permitAll()
                        .requestMatchers("/api/v1/products").permitAll()
                        
                        // Nếu muốn mở GET product public cho khách vãng lai thì thêm:
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recommendations/similar/**").permitAll()


                        // Các request còn lại yêu cầu JWT
                        .anyRequest().authenticated()
                )
                // Resource server JWT – sẽ tự dùng bean jwtDecoder() ở trên
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

    // CORS cho frontend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
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
