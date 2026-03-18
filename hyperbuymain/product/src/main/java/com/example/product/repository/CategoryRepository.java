package com.example.product.repository;

import com.example.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm category theo tên (phân biệt chữ hoa/thường)
    Optional<Category> findByName(String name);

    // Tìm category theo tên (không phân biệt chữ hoa/thường)
    Optional<Category> findByNameIgnoreCase(String name);

    // Tìm tất cả categories đang active
    List<Category> findByIsActiveTrue();

    // Bạn có thể thêm các phương thức truy vấn tùy chỉnh khác nếu cần
}