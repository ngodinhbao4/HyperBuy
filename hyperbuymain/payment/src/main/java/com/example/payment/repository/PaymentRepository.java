package com.example.payment.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.payment.entity.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(Long orderId);
    // Có thể thêm các phương thức truy vấn khác nếu cần
}