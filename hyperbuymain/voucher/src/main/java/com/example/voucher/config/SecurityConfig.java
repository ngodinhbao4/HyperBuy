package com.example.voucher.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;




@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // ✅ Cho phép các API public
                .requestMatchers(
                    "/vouchers",
                    "/vouchers/**",
                    "/voucher/vouchers",
                    "/voucher/vouchers/**",
                    "/voucher/vouchers/issue/**",
                    "/voucher/vouchers/user /**"
                ).permitAll()

                // ✅ API admin-only
                .requestMatchers("/voucher/vouchers").permitAll()
                .requestMatchers("/vouchers/redeem-by-points/**").permitAll()
                .requestMatchers("/vouchers/user/**").permitAll()
                .requestMatchers("/vouchers/redeem-by-points/**").permitAll()
                // PHÁT VOUCHER CHO USER (từ minigame)
                .requestMatchers("/vouchers/issue/**").permitAll()
                // LẤY VOUCHER CỦA USER
                .requestMatchers("/vouchers/user/**").permitAll()
                 .requestMatchers(
                    "/voucher/vouchers/redeem-by-points/**"
                ).permitAll()


                // ✅ Còn lại cần xác thực
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied"))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedOrigins(List.of("http://127.0.0.1:5500", "http://localhost:5500"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
