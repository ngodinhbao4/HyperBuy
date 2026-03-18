package com.example.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.user.entity.SellerRequestEntity;
import com.example.user.entity.User;

public interface SellerRequestRepository extends JpaRepository<SellerRequestEntity, String> {
    @Query("SELECT sr FROM SellerRequestEntity sr JOIN FETCH sr.user WHERE sr.status = 'PENDING'")
    List<SellerRequestEntity> findAllPendingWithUser();

    @Query("SELECT sr FROM SellerRequestEntity sr WHERE sr.user = :user AND sr.status = 'PENDING'")
    List<SellerRequestEntity> findPendingByUser(User user);
}
