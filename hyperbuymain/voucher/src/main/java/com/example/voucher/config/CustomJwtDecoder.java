package com.example.voucher.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder {

    private static final String SIGNER_KEY =
            "!TJXchW5FLOeSBb63Kck+DFHTaRpWL4JUGcWFgWxUG5S1F/ly/LgJxHnMQaF46A/i"; // giống user-service

    public Claims decode(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SIGNER_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
