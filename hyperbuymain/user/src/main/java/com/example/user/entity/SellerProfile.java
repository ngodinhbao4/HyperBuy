package com.example.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    String storeName;
    String businessLicense;
    @OneToOne(fetch = FetchType.EAGER)
    User user;
}
