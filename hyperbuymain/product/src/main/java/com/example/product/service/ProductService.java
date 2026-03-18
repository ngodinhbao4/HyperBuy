package com.example.product.service;

import com.example.product.dto.request.ProductRequest;
import com.example.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest, MultipartFile imageFile, String token);
    ProductResponse getProductById(Long id, String token);
    Page<ProductResponse> findProducts(Long categoryId, String nameQuery, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable, String token);
    ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile imageFile, String token);
    void deleteProduct(Long id, String storeId);
    ProductResponse updateStock(Long id, int quantityChange, String token);
    void activateProduct(Long id, String storeId);
    void deactivateProduct(Long id, String storeId);
    Page<ProductResponse> findMyProducts(Long categoryId, String nameQuery, Pageable pageable, String token);
    List<Map<String, Object>> findProductsByStoreId(String storeId, Pageable pageable);
}