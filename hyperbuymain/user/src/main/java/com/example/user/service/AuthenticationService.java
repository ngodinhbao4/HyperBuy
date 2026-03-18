package com.example.user.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.example.user.dto.request.AuthenticationRequest;
import com.example.user.dto.request.IntrospectRequest;
import com.example.user.dto.request.LogOutRequest;
import com.example.user.dto.response.AuthenticationResponse;
import com.example.user.dto.response.IntrospectResponse;
import com.example.user.entity.InvalidatedToken;
import com.example.user.entity.User;
import com.example.user.exception.AppException;
import com.example.user.exception.ErrorCode;
import com.example.user.repository.InvalidatedReponsitory;
import com.example.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedReponsitory invalidatedResponsitory;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;


    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException{
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token);
        } catch (AppException e) {
           isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

        public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.isBanned()) {
            log.warn("User {} is banned", request.getUsername());
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated)
            throw new AppException(ErrorCode.WRONG_PASSWORD);
    
        var token = generateToken(user);
        return AuthenticationResponse.builder()
            .token(token)
            .authenticated(true)
            .build();
    }

    private String generateToken(User user){
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .subject(user.getUsername())
            .issuer("hyperbuy.com")
            .issueTime(new Date())
            .expirationTime(new Date(
                Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
            ))
            .jwtID(UUID.randomUUID().toString())
            .claim("scope", buildScope(user))
            .build();
        
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Can not create token", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user){
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRole()))
            user.getRole().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if(!CollectionUtils.isEmpty(role.getPermission()))
                    role.getPermission()
                        .forEach(permission -> stringJoiner.add(permission.getName()));
            });
    
        return stringJoiner.toString();
    }

    public void logOut(LogOutRequest request) throws JOSEException, ParseException{
        var signToken = verifyToken(request.getToken());

        String jit = signToken.getJWTClaimsSet().getJWTID();
        Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(expiryTime)
                .build();

        invalidatedResponsitory.save(invalidatedToken);
    }

    private SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedResponsitory
            .existsById(signedJWT.getJWTClaimsSet().getJWTID()))
        throw new AppException(ErrorCode.UNAUTHENTICATED);
        return signedJWT;
    }
}