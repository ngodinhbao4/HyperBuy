// Trong order-service/src/main/java/com/yourapp/orderservice/exception/GlobalOrderExceptionHandler.java
package com.example.order.exception;

import com.example.order.dto.response.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException; // Nếu dùng RestTemplate
import org.springframework.web.context.request.WebRequest; // Thêm WebRequest

import java.util.stream.Collectors; // Thêm Collectors nếu bạn muốn dùng ErrorDetails như cũ


@ControllerAdvice
@Slf4j
public class GlobalOrderExceptionHandler {

    // Xử lý OrderException tùy chỉnh
    @ExceptionHandler(value = OrderException.class)
    public ResponseEntity<ApiResponse<?>> handlingOrderException(OrderException exception) {
        ErrorCodeOrder errorCode = exception.getErrorCodeOrder();
        log.error("OrderException occurred: Code - {}, Message - {}", errorCode.getCode(), exception.getMessage(), exception);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(exception.getMessage()) // Sử dụng message từ exception
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // --- Các ExceptionHandler đã có từ ví dụ trước, điều chỉnh để dùng ErrorCodeOrder và ApiResponse ---

    @ExceptionHandler(ResourceNotFoundException.class) // Exception này ta đã tạo trước đó
    public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("ResourceNotFoundException: {}", ex.getMessage());
        ErrorCodeOrder errorCode = ErrorCodeOrder.ORDER_NOT_FOUND; // Hoặc một mã lỗi cụ thể hơn nếu ResourceNotFoundException có thể phân biệt
        // Nếu ResourceNotFoundException có thể là PRODUCT_NOT_FOUND_FOR_ORDER, bạn cần logic để phân biệt
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("product")) {
             errorCode = ErrorCodeOrder.PRODUCT_NOT_FOUND_FOR_ORDER;
        }

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(ex.getMessage()) // Giữ message gốc của exception
                .build();
        return new ResponseEntity<>(apiResponse, errorCode.getStatusCode());
    }

    @ExceptionHandler(InsufficientStockException.class) // Exception này ta đã tạo trước đó
    public ResponseEntity<ApiResponse<?>> handleInsufficientStockException(InsufficientStockException ex, WebRequest request) {
        log.warn("InsufficientStockException: {}", ex.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeOrder.INSUFFICIENT_STOCK_FOR_ORDER.getCode())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, ErrorCodeOrder.INSUFFICIENT_STOCK_FOR_ORDER.getStatusCode());
    }

    @ExceptionHandler(EmptyCartException.class) // Exception này ta đã tạo trước đó
    public ResponseEntity<ApiResponse<?>> handleEmptyCartException(EmptyCartException ex, WebRequest request) {
        log.warn("EmptyCartException: {}", ex.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeOrder.EMPTY_CART_FOR_ORDER.getCode())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, ErrorCodeOrder.EMPTY_CART_FOR_ORDER.getStatusCode());
    }

    // Xử lý lỗi validation (ví dụ từ @Valid trong DTO request)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handlingValidation(MethodArgumentNotValidException exception) {
        String errorMessage = "Validation failed";
        if (exception.getBindingResult().hasErrors() && exception.getBindingResult().getFieldError() != null) {
            errorMessage = exception.getBindingResult().getFieldErrors().stream()
                             .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                             .collect(Collectors.joining("; "));
        }
        log.warn("MethodArgumentNotValidException: {}", errorMessage);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeOrder.INVALID_ORDER_REQUEST.getCode())
                .message(errorMessage)
                .build();
        return ResponseEntity.status(ErrorCodeOrder.INVALID_ORDER_REQUEST.getStatusCode()).body(apiResponse);
    }

    // Xử lý lỗi khi gọi các service khác bằng Feign (ví dụ ProductService, CartService)
    @ExceptionHandler(value = FeignException.class)
    public ResponseEntity<ApiResponse<?>> handlingFeignException(FeignException exception, WebRequest request) {
        String serviceName = "external service";
        // Cố gắng xác định service nào gây lỗi dựa trên request hoặc cấu hình Feign client
        // (Điều này có thể phức tạp và tùy thuộc vào cách bạn cấu hình Feign)
        // Ví dụ đơn giản:
        if (request.getDescription(false).contains("product")) { // Hoặc kiểm tra target của Feign client
            serviceName = "Product Service";
        } else if (request.getDescription(false).contains("cart")) {
            serviceName = "Cart Service";
        }

        log.error("FeignException occurred while communicating with {}: Status - {}, Message - {}", serviceName, exception.status(), exception.getMessage(), exception);
        ErrorCodeOrder errorCode;
        String specificMessage = "An error occurred while communicating with " + serviceName + ".";

        // Phân tích exception.status() hoặc loại FeignException
        if (exception instanceof FeignException.NotFound) {
            // Tùy vào service nào, mã lỗi có thể khác nhau
            errorCode = ErrorCodeOrder.PRODUCT_NOT_FOUND_FOR_ORDER; // Hoặc một mã lỗi chung hơn như RESOURCE_NOT_FOUND_FROM_EXTERNAL_SERVICE
            specificMessage = "The requested resource was not found from " + serviceName + ".";
        } else if (exception instanceof FeignException.ServiceUnavailable ||
                   exception instanceof FeignException.BadGateway ||
                   exception.status() == HttpStatus.SERVICE_UNAVAILABLE.value() ||
                   exception.status() == HttpStatus.BAD_GATEWAY.value()) {
            // Tùy vào service nào
            if (serviceName.contains("Cart")) errorCode = ErrorCodeOrder.CART_SERVICE_UNREACHABLE;
            else errorCode = ErrorCodeOrder.PRODUCT_SERVICE_UNREACHABLE_FOR_ORDER; // Default
            specificMessage = serviceName + " is currently unavailable.";
        } else if (exception.status() == HttpStatus.BAD_REQUEST.value()) {
             errorCode = ErrorCodeOrder.INVALID_ORDER_REQUEST; // Hoặc lỗi cụ thể từ service ngoài
             specificMessage = "Bad request sent to " + serviceName + ".";
             if (!exception.contentUTF8().isEmpty()){
                specificMessage += " Details: " + exception.contentUTF8();
             }
        }
        else {
            errorCode = ErrorCodeOrder.UNCATEGORIZED_ORDER_EXCEPTION; // Mã lỗi chung cho lỗi từ service ngoài
        }

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(specificMessage)
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Xử lý lỗi ResourceAccessException (thường khi không kết nối được service qua RestTemplate)
    @ExceptionHandler(value = ResourceAccessException.class)
    public ResponseEntity<ApiResponse<?>> handlingResourceAccessException(ResourceAccessException exception) {
        log.error("ResourceAccessException occurred: {}", exception.getMessage(), exception);
        // Cần xác định service nào không kết nối được nếu có thể
        ErrorCodeOrder errorCode = ErrorCodeOrder.PRODUCT_SERVICE_UNREACHABLE_FOR_ORDER; // Giả sử là Product Service
        // Bạn có thể cải thiện bằng cách xem xét thông điệp lỗi để đoán service
        if (exception.getMessage() != null) {
            if (exception.getMessage().toLowerCase().contains("cart")) {
                 errorCode = ErrorCodeOrder.CART_SERVICE_UNREACHABLE;
            }
        }
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Could not connect to " + (errorCode == ErrorCodeOrder.CART_SERVICE_UNREACHABLE ? "Cart Service" : "Product Service") + ". " + exception.getLocalizedMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // Xử lý lỗi chung nhất (nên để ở cuối)
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<?>> handlingGenericException(Exception exception) {
        log.error("Unhandled Exception occurred: {}", exception.getMessage(), exception); // Log cả stacktrace
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCodeOrder.UNCATEGORIZED_ORDER_EXCEPTION.getCode())
                .message(ErrorCodeOrder.UNCATEGORIZED_ORDER_EXCEPTION.getMessage() + (exception.getMessage() != null ? ": " + exception.getMessage() : ""))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}