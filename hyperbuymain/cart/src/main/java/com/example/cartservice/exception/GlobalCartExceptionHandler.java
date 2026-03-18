// Package: com.example.cartservice.exception (hoặc com.example.cartservice.controller.advice)
package com.example.cartservice.exception;

import com.example.cartservice.dto.response.ApiResponse; // Đảm bảo import đúng DTO response của bạn

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import feign.FeignException; // Nếu bạn dùng Feign để gọi các service khác

@ControllerAdvice
@Slf4j // Thêm Slf4j để log lỗi
public class GlobalCartExceptionHandler {

    // Xử lý CartException tùy chỉnh
    @ExceptionHandler(value = CartException.class)
    public ResponseEntity<ApiResponse<?>> handlingCartException(CartException exception) {
        ErrorCodeCart errorCode = exception.getErrorCodeCart();
        log.error("CartException occurred: Code - {}, Message - {}", errorCode.getCode(), exception.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(exception.getMessage()) // Sử dụng message từ exception, có thể đã được tùy chỉnh
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Xử lý lỗi validation (ví dụ từ @Valid trong DTO request)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException exception) {
        String errorMessage = "Validation failed";
        if (exception.getBindingResult().hasErrors() && exception.getBindingResult().getFieldError() != null) {
            errorMessage = exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        log.warn("MethodArgumentNotValidException: {}", errorMessage);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeCart.INVALID_CART_REQUEST.getCode()) // Hoặc một mã lỗi validation cụ thể hơn
                .message(errorMessage)
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
    }

    // Xử lý lỗi khi gọi các service khác bằng Feign (ví dụ ProductService, UserService)
    @ExceptionHandler(value = FeignException.class)
    public ResponseEntity<ApiResponse<?>> handlingFeignException(FeignException exception) {
        log.error("FeignException occurred: Status - {}, Message - {}", exception.status(), exception.getMessage());
        ErrorCodeCart errorCode;
        String specificMessage = "An error occurred while communicating with an external service.";

        // Bạn có thể phân tích exception.status() để trả về mã lỗi cụ thể hơn
        if (exception instanceof FeignException.NotFound) {
            errorCode = ErrorCodeCart.PRODUCT_NOT_AVAILABLE; // Hoặc một mã lỗi chung hơn
            specificMessage = "The requested external resource was not found.";
        } else if (exception instanceof FeignException.ServiceUnavailable || exception instanceof FeignException.BadGateway ) {
            errorCode = ErrorCodeCart.PRODUCT_SERVICE_UNREACHABLE; // Hoặc một mã lỗi chung hơn
            specificMessage = "External service is currently unavailable.";
        } else {
            errorCode = ErrorCodeCart.UNCATEGORIZED_CART_EXCEPTION; // Hoặc một mã lỗi chung cho lỗi service ngoài
        }

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(specificMessage + (exception.contentUTF8().isEmpty() ? "" : " Details: " + exception.contentUTF8()))
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Xử lý lỗi ResourceAccessException (thường khi không kết nối được service)
    @ExceptionHandler(value = ResourceAccessException.class)
    public ResponseEntity<ApiResponse<?>> handlingResourceAccessException(ResourceAccessException exception) {
        log.error("ResourceAccessException occurred: {}", exception.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeCart.PRODUCT_SERVICE_UNREACHABLE.getCode()) // Hoặc một mã lỗi chung
                .message("Could not connect to an external service: " + exception.getMessage())
                .build();
        return ResponseEntity.status(ErrorCodeCart.PRODUCT_SERVICE_UNREACHABLE.getStatusCode()).body(apiResponse);
    }
    
    // Xử lý lỗi chung nhất (nên để ở cuối)
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<?>> handlingRuntimeException(Exception exception) {
        log.error("Unhandled RuntimeException occurred: {}", exception.getMessage(), exception); // Log cả stacktrace
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeCart.UNCATEGORIZED_CART_EXCEPTION.getCode())
                .message(ErrorCodeCart.UNCATEGORIZED_CART_EXCEPTION.getMessage() + ": " + exception.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}