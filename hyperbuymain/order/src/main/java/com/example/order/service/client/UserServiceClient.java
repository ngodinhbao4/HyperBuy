package com.example.order.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.order.dto.response.ApiResponRequest;
import com.example.order.dto.response.UserResponse;

@FeignClient(name = "userservice", url = "${user.service.url}")
public interface UserServiceClient {
    @GetMapping("/user/users/username/{username}")
    ApiResponRequest<UserResponse> getUserByUsername(@PathVariable("username") String username, @RequestHeader("Authorization") String authorizationHeader);
}