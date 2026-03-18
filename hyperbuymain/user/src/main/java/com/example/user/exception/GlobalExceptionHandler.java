package com.example.user.exception;



import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.user.dto.request.ApiResponRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponRequest<Object>> handlingRuntimeException(RuntimeException exception){

        ApiResponRequest<Object> apiResponRequest = new ApiResponRequest<>();

        apiResponRequest.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponRequest.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(apiResponRequest);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponRequest<Object>> handlingAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponRequest<Object> apiResponRequest = new ApiResponRequest<>();

        apiResponRequest.setCode(errorCode.getCode());
        apiResponRequest.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponRequest);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponRequest<Object>> handlingAccessDeniedException(AccessDeniedException exception){
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode()).body(
            ApiResponRequest.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponRequest<Object>> handlingValidation(MethodArgumentNotValidException exception){

        String enumKey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(enumKey);

        ApiResponRequest<Object> apiResponRequest = new ApiResponRequest<>();

        apiResponRequest.setCode(errorCode.getCode());
        apiResponRequest.setMessage(errorCode.getMessage());

        return ResponseEntity.badRequest().body(apiResponRequest);
    }
}
