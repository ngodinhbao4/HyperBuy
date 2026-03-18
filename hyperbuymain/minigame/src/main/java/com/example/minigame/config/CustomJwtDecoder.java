package com.example.minigame.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * ✅ Giải mã JWT để lấy thông tin user từ UserService
 */
@Slf4j
@Component
public class CustomJwtDecoder {

    @Value("${jwt.signerKey}")
    private String jwtSignerKey;

    public Authentication decode(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSignerKey.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities =
                    role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : Collections.emptyList();

            return new UsernamePasswordAuthenticationToken(username, null, authorities);

        } catch (ExpiredJwtException e) {
            log.warn("⚠️ Token đã hết hạn: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Lỗi khi giải mã JWT: {}", e.getMessage());
            return null;
        }
    }
}
    