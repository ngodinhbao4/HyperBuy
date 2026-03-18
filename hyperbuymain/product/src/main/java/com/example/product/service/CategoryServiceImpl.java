package com.example.product.service; // Giữ nguyên package của bạn

import com.example.product.dto.request.CategoryRequest;
import com.example.product.dto.response.CategoryResponse;
import com.example.product.dto.response.ProductResponse;
import com.example.product.entity.Category;
import com.example.product.entity.Product;
// Import AppException và ErrorCode
import com.example.product.exception.AppException;
import com.example.product.exception.ErrorCode;
import com.example.product.repository.CategoryRepository;
import com.example.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository; 

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        categoryRepository.findByNameIgnoreCase(categoryRequest.getName()).ifPresent(existing -> {
            // Ném AppException với ErrorCode tương ứng
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTED);
        });

        Category category = mapToEntity(categoryRequest);
        category.setActive(true);
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        CategoryResponse response = mapToResponse(category);

        // Lấy sản phẩm active và chưa bị xóa
        List<ProductResponse> products = productRepository
                .findByCategoryIdAndIsDeletedFalseAndIsActiveTrue(id, null)
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());

        response.setProducts(products);
        return response;
    }

    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setSku(product.getSku());
        response.setStockQuantity(product.getStockQuantity());
        response.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        response.setActive(product.isActive());
        response.setImageUrl(product.getImageUrl());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(boolean activeOnly) {
        List<Category> categories;
        if (activeOnly) {
            categories = categoryRepository.findByIsActiveTrue();
        } else {
            categories = categoryRepository.findAll();
        }
        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        categoryRepository.findByNameIgnoreCase(categoryRequest.getName()).ifPresent(found -> {
            if (!found.getId().equals(id)) {
                throw new AppException(ErrorCode.CATEGORY_NAME_EXISTED);
            }
        });

        existingCategory.setName(categoryRequest.getName());
        existingCategory.setDescription(categoryRequest.getDescription());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapToResponse(updatedCategory);
    }

   public void deleteCategory(Long id) {
        // 1. Tìm Category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. Kiểm tra sự tồn tại của Product thuộc Category này
        // (Sử dụng phương thức mới trong ProductRepository)
        boolean hasProducts = productRepository.existsByCategoryIdAndIsDeletedFalseAndIsActiveTrue(id);

        // 3. Nếu có Product, ném lỗi
        if (hasProducts) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }

        // 4. Nếu không có Product, tiến hành xóa mềm (deactivate)
        category.setActive(false);
        categoryRepository.save(category);
    }
    

    @Override
    @Transactional
    public void activateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setActive(true);
        categoryRepository.save(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setActive(category.isActive());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }

    private Category mapToEntity(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return category;
    }

    @Override
    @Transactional
    public void permanentlyDeleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Kiểm tra xem category có sản phẩm active và chưa bị xóa hay không
        boolean hasProducts = productRepository.existsByCategoryIdAndIsDeletedFalseAndIsActiveTrue(id);
        if (hasProducts) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }

        // Xóa vĩnh viễn category
        categoryRepository.delete(category);
    }
}