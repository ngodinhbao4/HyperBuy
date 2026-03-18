package com.example.product.repository;

import com.example.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySkuAndIsDeletedFalseAndIsActiveTrue(String sku);

    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    Optional<Product> findById(Long id);

    Page<Product> findByIsDeletedFalseAndIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.isActive = true " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:nameQuery IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :nameQuery, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
            @Param("categoryId") Long categoryId,
            @Param("nameQuery") String nameQuery,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    Page<Product> findByCategoryIdAndIsDeletedFalseAndIsActiveTrue(Long categoryId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndIsDeletedFalseAndIsActiveTrue(String nameQuery, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND p.isActive = true AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:nameQuery IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :nameQuery, '%')))")
    Page<Product> searchProductsGeneral(
            @Param("categoryId") Long categoryId,
            @Param("nameQuery") String nameQuery,
            Pageable pageable
    );

         boolean existsByCategoryIdAndIsDeletedFalseAndIsActiveTrue(Long categoryId);

        Page<Product> findByStoreIdAndIsDeletedFalseAndIsActiveTrue(String storeId, Pageable pageable);
}