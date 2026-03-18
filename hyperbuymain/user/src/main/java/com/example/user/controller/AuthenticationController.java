package com.example.user.controller;

import java.text.ParseException;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user.dto.request.ApiResponRequest;
import com.example.user.dto.request.AuthenticationRequest;
import com.example.user.dto.request.IntrospectRequest;
import com.example.user.dto.request.LogOutRequest;
import com.example.user.dto.response.AuthenticationResponse;
import com.example.user.dto.response.IntrospectResponse;
import com.example.user.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    
    @PostMapping("/signin")
    ApiResponRequest<AuthenticationResponse> authencate(@RequestBody AuthenticationRequest request){
        var result = authenticationService.authenticate(request);

        return ApiResponRequest.<AuthenticationResponse>builder()
            .result(result)
            .build();
        
    }

    @PostMapping("/introspect")
    ApiResponRequest<IntrospectResponse> authencate(@RequestBody IntrospectRequest request) throws JOSEException, ParseException{
        var result = authenticationService.introspect(request);

        return ApiResponRequest.<IntrospectResponse>builder()
            .result(result)
            .build();
        
    }

    @PostMapping("/logout")
    ApiResponRequest<Void> logout(@RequestBody LogOutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logOut(request);
        return ApiResponRequest.<Void>builder()
                .build();
}
}