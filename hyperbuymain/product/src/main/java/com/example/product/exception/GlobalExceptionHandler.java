package com.example.product.exception; // Giữ nguyên package của bạn

import java.nio.file.AccessDeniedException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.product.dto.response.ApiResponse;

// Đảm bảo class này tồn tại và đúng
// import lombok.extern.slf4j.Slf4j; // Nên thêm logging

// @Slf4j // Nên thêm logging
@ControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý Exception chung nhất (nên là RuntimeException để tránh bắt cả checked exceptions không mong muốn)
    @ExceptionHandler(value = RuntimeException.class) // Đổi từ Exception sang RuntimeException
    ResponseEntity<ApiResponse<Object>> handlingRuntimeException(RuntimeException exception){
        // log.error("Uncategorized Exception: ", exception); // Log lỗi đầy đủ
        ApiResponse<Object> apiResponRequest = new ApiResponse<>();
        apiResponRequest.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponRequest.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        // Nên trả về status code của UNCATEGORIZED_EXCEPTION
        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode()).body(apiResponRequest);
    }

    // Xử lý AppException tùy chỉnh của bạn
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Object>> handlingAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Object> apiResponRequest = new ApiResponse<>();
        apiResponRequest.setCode(errorCode.getCode());
        apiResponRequest.setMessage(errorCode.getMessage());
        // log.warn("AppException handled: Code - {}, Message - {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponRequest);
    }

    // Xử lý AccessDeniedException (nếu dùng Spring Security)
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<Object>> handlingAccessDeniedException(AccessDeniedException exception){
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED; // Hoặc ErrorCode.FORBIDDEN tùy ngữ cảnh
        // log.warn("Access Denied: {}", exception.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(
            ApiResponse.builder() // Giả sử ApiResponRequest có builder
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
        );
    }

    // Xử lý lỗi validation từ @Valid
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handlingValidation(MethodArgumentNotValidException exception){
        // Cách hiện tại của bạn: lấy message làm key cho ErrorCode
        // String enumKey = exception.getFieldError().getDefaultMessage();
        // ErrorCode errorCode = ErrorCode.valueOf(enumKey); // Cách này yêu cầu message phải khớp chính xác với tên enum

        // Cách tiếp cận an toàn hơn: Trả về thông điệp lỗi validation trực tiếp hơn là map sang ErrorCode
        // Hoặc nếu bạn muốn dùng ErrorCode cụ thể cho từng lỗi validation đã định nghĩa ở trên:
        String defaultMessage = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode;
        try {
            // Thử tìm ErrorCode dựa trên message (như cách bạn đang làm)
            // Cần đảm bảo message trong @NotNull, @NotBlank, v.v. khớp với tên enum ErrorCode
            errorCode = ErrorCode.valueOf(defaultMessage);
        } catch (IllegalArgumentException e) {
            // Nếu không tìm thấy ErrorCode khớp, dùng một mã lỗi chung cho validation
            // Hoặc xử lý chi tiết hơn:
            // log.warn("Validation error message not mapped to ErrorCode: {}", defaultMessage);
            ApiResponse<Object> apiResponRequest = new ApiResponse<>();
            apiResponRequest.setCode(ErrorCode.INVALID_KEY.getCode()); // Mã lỗi chung cho bad request
            // Lấy thông điệp chi tiết từ lỗi validation đầu tiên
            apiResponRequest.setMessage(exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(apiResponRequest);
        }


        ApiResponse<Object> apiResponRequest = new ApiResponse<>();
        apiResponRequest.setCode(errorCode.getCode());
        apiResponRequest.setMessage(errorCode.getMessage()); // Message từ ErrorCode
        // log.warn("Validation Error: Code - {}, Message - {}", errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponRequest);
    }

    // (Nên thêm) Xử lý DataIntegrityViolationException
    // @ExceptionHandler(DataIntegrityViolationException.class)
    // public ResponseEntity<ApiResponRequest<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
    //     // log.error("Data Integrity Violation: ", ex);
    //     ApiResponRequest<Object> apiResponRequest = new ApiResponRequest<>();
    //     ErrorCode errorCode = ErrorCode.PRODUCT_SKU_EXISTED; // Hoặc một mã lỗi chung cho data conflict
    //
    //     // Cố gắng lấy thông tin chi tiết hơn từ exception (tùy thuộc vào DB và loại constraint)
    //     // String specificMessage = ex.getMostSpecificCause().getMessage();
    //     // if (specificMessage.contains("some_unique_constraint_name_for_sku")) {
    //     //     errorCode = ErrorCode.PRODUCT_SKU_EXISTED;
    //     // } else if (specificMessage.contains("some_unique_constraint_name_for_category_name")) {
    //     //     errorCode = ErrorCode.CATEGORY_NAME_EXISTED;
    //     // }
    //
    //     apiResponRequest.setCode(errorCode.getCode());
    //     apiResponRequest.setMessage(errorCode.getMessage());
    //     return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponRequest);
    // }
}