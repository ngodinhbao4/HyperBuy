package com.example.user.repository;

import com.example.user.entity.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, String> {
    Optional<SellerProfile> findByUserUsername(String username);
    Optional<SellerProfile> findById(String id); // Thêm phương thức tìm bằng storeId
}