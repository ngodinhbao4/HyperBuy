package com.example.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.signer-key}")
    private String signerKey;

   @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Áp dụng CORS
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Cho phép tất cả preflight request
            .requestMatchers("/chat/**").permitAll() // Cho phép WebSocket endpoint
            .requestMatchers(HttpMethod.POST, "/api/v1/chat/conversations").authenticated() // POST để tạo cuộc trò chuyện cần xác thực
            .requestMatchers(HttpMethod.GET, "/api/v1/chat/conversations").permitAll() // GET để lấy cuộc trò chuyện
            .requestMatchers(HttpMethod.PATCH, "/api/v1/chat/conversations").permitAll() // PATCH để cập nhật
            .requestMatchers(HttpMethod.POST, "/api/v1/chat/**").authenticated() // POST cho các endpoint khác cần xác thực
            .requestMatchers(HttpMethod.GET, "/api/v1/chat/**").permitAll() // GET cho các endpoint khác
            .requestMatchers(HttpMethod.PATCH, "/api/v1/chat/**").permitAll() // PATCH cho các endpoint khác
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.decoder(jwtDecoder()))
        );
    return http.build();
}

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(signerKey.getBytes(), "HmacSHA512");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();

        return token -> {
            Jwt jwt = decoder.decode(token);
            // Kiểm tra issuer từ claims
            String issuer = (String) jwt.getClaims().get("iss");
            if (!"hyperbuy.com".equals(issuer)) {
                throw new JwtException("Invalid issuer: " + issuer);
            }
            return jwt;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép các origin cụ thể
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:5500"));
        // Hỗ trợ các phương thức HTTP cần thiết
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Cho phép các header cần thiết
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        // Cho phép gửi credentials (như token)
        configuration.setAllowCredentials(true);
        

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}