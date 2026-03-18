// Trong order-service/src/main/java/com/example/order/exception/ResourceNotFoundException.java
package com.example.order.exception; // Hoặc package của bạn

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}