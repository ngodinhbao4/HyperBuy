// Trong order-service/src/main/java/com/example/order/exception/InsufficientStockException.java
package com.example.order.exception; // Hoặc package của bạn

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}