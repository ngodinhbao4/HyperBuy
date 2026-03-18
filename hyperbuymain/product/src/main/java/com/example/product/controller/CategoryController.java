package com.example.product.controller;

import com.example.product.dto.request.CategoryRequest;
import com.example.product.dto.response.ApiResponse;
import com.example.product.dto.response.CategoryResponse;
import com.example.product.exception.AppException;
import com.example.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories") // Base path cho category API
@RequiredArgsConstructor // Lombok constructor injection
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryResponse createdCategory = categoryService.createCategory(categoryRequest);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category); // Bao gồm sản phẩm
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
        List<CategoryResponse> categories = categoryService.getAllCategories(activeOnly);
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                       @Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryResponse updatedCategory = categoryService.updateCategory(id, categoryRequest);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        // Thực hiện soft delete (thực chất là deactive category trong service)
        categoryService.deleteCategory(id);
        // Trả về 204 No Content vì không có body nào cần trả về
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate") // Dùng PATCH để cập nhật một phần trạng thái
    public ResponseEntity<Void> activateCategory(@PathVariable Long id) {
        categoryService.activateCategory(id);
        return ResponseEntity.ok().build(); // Trả về 200 OK (hoặc 204 No Content cũng được)
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteCategory(@PathVariable Long id) {
        try {
            categoryService.permanentlyDeleteCategory(id);
            ApiResponse<Void> response = ApiResponse.<Void>builder()
                    .code(1000)
                    .message("Xóa vĩnh viễn danh mục thành công")
                    .build();
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
                    .code(e.getErrorCode().getCode())
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}