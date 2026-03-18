package com.example.user.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String token = null;
            if (principal instanceof Jwt jwt) {
                token = jwt.getTokenValue();
            }
            if (token != null) {
                template.header("Authorization", "Bearer " + token);
                template.header("Content-Type", "application/json");
                // Log để kiểm tra token
                System.out.println("Feign Client Token: Bearer " + token);
            } else {
                System.out.println("No JWT token found in SecurityContext");
            }
        };
    }
}