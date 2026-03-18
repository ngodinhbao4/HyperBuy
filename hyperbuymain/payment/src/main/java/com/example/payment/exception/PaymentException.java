// File: payment-service/src/main/java/com/example/payment/exception/PaymentException.java
package com.example.payment.exception;
public class PaymentException extends RuntimeException {

    private final ErrorCodePayment errorCodePayment;

    public PaymentException(ErrorCodePayment errorCodePayment) {
        super(errorCodePayment.getMessage());
        this.errorCodePayment = errorCodePayment;
    }

    public PaymentException(ErrorCodePayment errorCodePayment, String customMessage) {
        super(customMessage); // Sử dụng customMessage nếu được cung cấp
        this.errorCodePayment = errorCodePayment;
    }

    // Constructor để bao bọc một exception gốc
    public PaymentException(ErrorCodePayment errorCodePayment, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCodePayment = errorCodePayment;
    }

     public PaymentException(ErrorCodePayment errorCodePayment, Throwable cause) {
        super(errorCodePayment.getMessage(), cause);
        this.errorCodePayment = errorCodePayment;
    }

    public ErrorCodePayment getErrorCodePayment() {
        return errorCodePayment;
    }
}
