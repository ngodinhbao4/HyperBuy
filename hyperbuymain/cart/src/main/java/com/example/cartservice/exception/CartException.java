// Package: com.example.cartservice.exception
package com.example.cartservice.exception;

public class CartException extends RuntimeException {

    private final ErrorCodeCart errorCodeCart;

    public CartException(ErrorCodeCart errorCodeCart) {
        super(errorCodeCart.getMessage());
        this.errorCodeCart = errorCodeCart;
    }

    public CartException(ErrorCodeCart errorCodeCart, String customMessage) {
        super(customMessage);
        this.errorCodeCart = errorCodeCart;
    }

    public ErrorCodeCart getErrorCodeCart() {
        return errorCodeCart;
    }
}