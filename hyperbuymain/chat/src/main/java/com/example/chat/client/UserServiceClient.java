package com.example.chat.client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.chat.dto.response.ApiResponse;

import java.util.logging.Logger;

@Service
public class UserServiceClient {

    private static final Logger LOGGER = Logger.getLogger(UserServiceClient.class.getName());

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate, @Value("${user-service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public boolean validateUser(String userId, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = userServiceUrl + "/user/users/" + userId;
            LOGGER.info("Calling UserService: " + url);
            restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class);
            LOGGER.info("User ID " + userId + " validated successfully");
            return true;
        } catch (HttpClientErrorException e) {
            LOGGER.severe("Error validating user ID " + userId + ": HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            LOGGER.severe("Unexpected error validating user ID " + userId + ": " + e.getMessage());
            return false;
        }
    }
}