package com.example.notification.client;

import com.example.notification.dto.UserResponse;
import com.example.notification.dto.response.ApiResponRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "userservice", url = "${user.service.url}")
public interface UserServiceClient {
    @GetMapping("user/users/{userId}")
    ApiResponRequest<UserResponse> getUserById(@PathVariable("userId") String userId, @RequestHeader("Authorization") String authorizationHeader);
}