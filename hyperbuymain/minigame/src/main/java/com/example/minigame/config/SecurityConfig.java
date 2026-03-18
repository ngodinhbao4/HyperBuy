package com.example.minigame.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


/**
 * ✅ Cấu hình Spring Security cho MiniGameService
 *  - Hỗ trợ JWT
 *  - Cho phép public các API chơi game
 *  - Bảo vệ các API đổi thưởng, vòng quay
 *  - Tránh vòng lặp /error 403
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // 🚫 Tắt CSRF vì sử dụng JWT
            .csrf(csrf -> csrf.disable())

            // 🌐 Cho phép frontend gọi API từ cổng khác
            .cors(cors -> {})

            // 🔐 Phân quyền truy cập endpoint
            .authorizeHttpRequests(auth -> auth
                // ✅ Cho phép truy cập các API public
                .requestMatchers("/error").permitAll() // fix lỗi vòng lặp /error
                .requestMatchers("/actuator/**").permitAll() // cho phép health check (nếu có)
                .requestMatchers(
                        "/minigame/play",
                        "/minigame/daily-reward/**",
                        "/minigame/history/**",
                        "/minigame/summary"
                ).permitAll()

                .requestMatchers(
                    "/minigame/api/v1/loyalty/spend",
                    "/minigame/api/v1/loyalty/**"    // nếu muốn thoáng luôn cả group
                ).permitAll()
                .requestMatchers(
                    "/api/v1/loyalty/spend",
                    "/api/v1/loyalty/**"    // nếu muốn thoáng luôn cả group
                ).permitAll()

                // 🔒 Các API cần JWT xác thực
                .requestMatchers(
                        "/minigame/spin/**",
                        "/minigame/reward/**",
                        "/minigame/redeem/**"
                ).authenticated()

                // 🔒 Mặc định: yêu cầu xác thực
                .anyRequest().authenticated()
            )

            // ⚙️ Xử lý khi xác thực thất bại (thay vì redirect /error)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                            "error": "Unauthorized",
                            "message": "Bạn cần đăng nhập hoặc token không hợp lệ."
                        }
                        """);
                })
            )

            // 🧩 Thêm JWT filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

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
