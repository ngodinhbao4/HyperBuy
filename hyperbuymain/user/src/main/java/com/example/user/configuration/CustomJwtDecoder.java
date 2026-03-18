package com.example.user.configuration;

import com.example.user.repository.InvalidatedReponsitory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    private final InvalidatedReponsitory invalidatedReponsitory;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    public CustomJwtDecoder(InvalidatedReponsitory invalidatedReponsitory) {
        this.invalidatedReponsitory = invalidatedReponsitory;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder
                        .withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
            }

            Jwt jwt = nimbusJwtDecoder.decode(token);

            String jti = jwt.getId();
            if (invalidatedReponsitory.existsById(jti)) {
                throw new JwtException("Token invalid");
            }

            return jwt;
        } catch (JwtException e) {
            throw new JwtException("Token invalid: " + e.getMessage(), e);
        }
    }
}