// File: payment-service/src/main/java/com/example/payment/exception/GlobalPaymentExceptionHandler.java
package com.example.payment.exception;

// Đảm bảo import đúng DTO ApiResponse của PaymentService
import com.example.payment.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException; // Nếu dùng RestTemplate để gọi cổng thanh toán
// import feign.FeignException; // Nếu PaymentService gọi service khác bằng Feign

import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalPaymentExceptionHandler {

    /**
     * Xử lý PaymentException tùy chỉnh được ném từ tầng service.
     * @param exception Đối tượng PaymentException.
     * @return ResponseEntity chứa ApiResponse với chi tiết lỗi.
     */
    @ExceptionHandler(value = PaymentException.class)
    public ResponseEntity<ApiResponse<?>> handlingPaymentException(PaymentException exception) {
        ErrorCodePayment errorCode = exception.getErrorCodePayment();
        // Log message gốc của exception (có thể đã được tùy chỉnh khi ném exception)
        log.error("PaymentException occurred: Code - {}, Message - '{}'", errorCode.getCode(), exception.getMessage(), exception);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(exception.getMessage()) // Sử dụng message từ exception
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Xử lý lỗi validation từ @Valid trên các DTO request.
     * @param exception Đối tượng MethodArgumentNotValidException.
     * @return ResponseEntity chứa ApiResponse với chi tiết các lỗi validation.
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException exception) {
        String errorMessage = "Dữ liệu không hợp lệ"; // Thông điệp chung
        if (exception.getBindingResult().hasErrors()) {
            // Lấy tất cả các lỗi field và nối chúng lại, tương tự như GlobalCartExceptionHandler
            errorMessage = exception.getBindingResult().getFieldErrors().stream()
                             .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                             .collect(Collectors.joining("; "));
        }
        log.warn("MethodArgumentNotValidException: {}", errorMessage);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodePayment.INVALID_PAYMENT_REQUEST.getCode())
                .message(errorMessage)
                .build();
        return ResponseEntity.status(ErrorCodePayment.INVALID_PAYMENT_REQUEST.getStatusCode()).body(apiResponse);
    }

    /**
     * Xử lý lỗi khi gọi cổng thanh toán hoặc service ngoài bằng RestTemplate.
     * Handler này sẽ được kích hoạt nếu RestClientException không được bắt và gói lại
     * thành PaymentException ở tầng service.
     * @param exception Đối tượng RestClientException.
     * @return ResponseEntity chứa ApiResponse với chi tiết lỗi.
     */
    @ExceptionHandler(value = RestClientException.class)
    public ResponseEntity<ApiResponse<?>> handlingRestClientException(RestClientException exception) {
        log.error("RestClientException occurred (e.g., calling payment gateway or other external service): {}", exception.getMessage(), exception);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodePayment.PAYMENT_GATEWAY_ERROR.getCode()) // Hoặc một mã lỗi chung hơn cho lỗi giao tiếp
                .message("Lỗi giao tiếp với cổng thanh toán hoặc dịch vụ bên ngoài: " + exception.getMessage())
                .build();
        return ResponseEntity.status(ErrorCodePayment.PAYMENT_GATEWAY_ERROR.getStatusCode()).body(apiResponse);
    }

    // (Tùy chọn) Xử lý FeignException nếu PaymentService gọi các microservice khác bằng Feign
    // @ExceptionHandler(value = FeignException.class)
    // public ResponseEntity<ApiResponse<?>> handlingFeignException(FeignException exception) {
    //     log.error("FeignException occurred: Status - {}, Message - {}", exception.status(), exception.getMessage());
    //     ErrorCodePayment errorCode;
    //     String specificMessage = "Lỗi khi giao tiếp với dịch vụ nội bộ.";
    //
    //     if (exception instanceof FeignException.NotFound) {
    //         errorCode = ErrorCodePayment.ORDER_SERVICE_UNREACHABLE_FOR_PAYMENT; // Ví dụ
    //         specificMessage = "Không tìm thấy tài nguyên từ dịch vụ nội bộ.";
    //     } else if (exception instanceof FeignException.ServiceUnavailable || exception instanceof FeignException.BadGateway ) {
    //         errorCode = ErrorCodePayment.ORDER_SERVICE_UNREACHABLE_FOR_PAYMENT; // Ví dụ
    //         specificMessage = "Dịch vụ nội bộ hiện không khả dụng.";
    //     } else {
    //         errorCode = ErrorCodePayment.UNCATEGORIZED_PAYMENT_EXCEPTION;
    //     }
    //
    //     ApiResponse<?> apiResponse = ApiResponse.builder()
    //             .code(errorCode.getCode())
    //             .message(specificMessage + (exception.contentUTF8().isEmpty() ? "" : " Chi tiết: " + exception.contentUTF8()))
    //             .build();
    //     return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    // }
    
    /**
     * Xử lý các lỗi Exception chung nhất, không được bắt bởi các handler cụ thể khác.
     * @param exception Đối tượng Exception.
     * @return ResponseEntity chứa ApiResponse với thông tin lỗi chung.
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<?>> handlingGenericException(Exception exception) {
        log.error("Unhandled Exception occurred: {}", exception.getMessage(), exception); // Log cả stacktrace
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodePayment.UNCATEGORIZED_PAYMENT_EXCEPTION.getCode())
                .message(ErrorCodePayment.UNCATEGORIZED_PAYMENT_EXCEPTION.getMessage() + (exception.getMessage() != null ? ": " + exception.getMessage() : ""))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
