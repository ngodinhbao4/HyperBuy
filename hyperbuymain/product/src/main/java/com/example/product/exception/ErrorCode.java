package com.example.product.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
    UNAUTHORIZED(1007, "Bạn không có quyền", HttpStatus.FORBIDDEN),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
     // Lỗi cho Product Service (bắt đầu từ một dải mã mới, ví dụ 2xxx)
    PRODUCT_NOT_FOUND(2001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    CATEGORY_HAS_PRODUCTS(2007, "Không thể xóa danh mục này vì chứa sản phẩm", HttpStatus.BAD_REQUEST), // Hoặc HttpStatus.CONFLICT (409)
    CATEGORY_NOT_FOUND(2002, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    PRODUCT_SKU_EXISTED(2003, "SKU của sản phẩm này đã tồn tại", HttpStatus.BAD_REQUEST), // Hoặc CONFLICT (409)
    CATEGORY_NAME_EXISTED(2004, "Tên danh mục đã tồn tại", HttpStatus.BAD_REQUEST), // Hoặc CONFLICT (409)
    STOCK_QUANTITY_INVALID(2005, "Cập nhật số lượng hàng tồn kho không được > 0", HttpStatus.BAD_REQUEST),
    CANNOT_ACTIVATE_DELETED_PRODUCT(2006, "Không thể kích hoạt lại sản phẩm đã xóa", HttpStatus.BAD_REQUEST),

    // Lỗi validation (có thể dùng chung hoặc định nghĩa riêng cho từng trường)
    // Cách bạn đang làm (dùng message từ validation làm key cho ErrorCode) cũng là một hướng
    // Nhưng nếu muốn mã lỗi rõ ràng hơn cho từng trường, bạn có thể định nghĩa:
    PRODUCT_NAME_BLANK(2101, "Tên sản phẩm không được trống", HttpStatus.BAD_REQUEST),
    PRODUCT_PRICE_INVALID(2102, "Giá của sản phẩm phải > 0", HttpStatus.BAD_REQUEST),
    PRODUCT_STOCK_NEGATIVE(2103, "Số lượng sản phẩm phải > 0", HttpStatus.BAD_REQUEST),
    CATEGORY_ID_NULL(2104, "Danh mục không được trống", HttpStatus.BAD_REQUEST),
    // ... thêm các mã lỗi validation khác nếu cần
    FILE_STORAGE_INITIALIZATION_ERROR(3001, "Could not initialize storage location", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_STORAGE_ERROR(3002, "Could not store file", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_FILE_ERROR(3003, "Failed to store empty file", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND_ERROR(3004, "File not found or not readable", HttpStatus.NOT_FOUND),
    FILE_DELETION_ERROR(3005, "Could not delete file", HttpStatus.INTERNAL_SERVER_ERROR),
    MALFORMED_FILE_PATH_ERROR(3006, "Malformed file path", HttpStatus.INTERNAL_SERVER_ERROR);
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;
}