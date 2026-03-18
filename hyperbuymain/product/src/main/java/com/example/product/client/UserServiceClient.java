package com.example.product.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.product.dto.response.UserServiceResponse;

@FeignClient(name = "userservice", url = "http://userservice:8080")
public interface UserServiceClient {

    @GetMapping("/user/users/store/{storeId}")
    UserServiceResponse getUserByStoreId(
            @PathVariable("storeId") String storeId,
            @RequestHeader("Authorization") String token);
}