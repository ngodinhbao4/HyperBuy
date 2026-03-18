package com.example.product.service;

import com.example.product.dto.request.CategoryRequest;
import com.example.product.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse getCategoryById(Long id);
    List<CategoryResponse> getAllCategories(boolean activeOnly);
    CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest);
    void deleteCategory(Long id); // Soft delete
    void activateCategory(Long id);
    void permanentlyDeleteCategory(Long id); // Hard delete
}