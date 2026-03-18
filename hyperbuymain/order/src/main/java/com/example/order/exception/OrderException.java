// Trong order-service/src/main/java/com/yourapp/orderservice/exception/OrderException.java
package com.example.order.exception;

public class OrderException extends RuntimeException {

    private final ErrorCodeOrder errorCodeOrder;

    public OrderException(ErrorCodeOrder errorCodeOrder) {
        super(errorCodeOrder.getMessage());
        this.errorCodeOrder = errorCodeOrder;
    }

    public OrderException(ErrorCodeOrder errorCodeOrder, String customMessage) {
        super(customMessage); // Sử dụng customMessage nếu được cung cấp
        this.errorCodeOrder = errorCodeOrder;
    }

    // Constructor để bao bọc một exception gốc
    public OrderException(ErrorCodeOrder errorCodeOrder, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCodeOrder = errorCodeOrder;
    }

     public OrderException(ErrorCodeOrder errorCodeOrder, Throwable cause) {
        super(errorCodeOrder.getMessage(), cause);
        this.errorCodeOrder = errorCodeOrder;
    }

    public ErrorCodeOrder getErrorCodeOrder() {
        return errorCodeOrder;
    }
}