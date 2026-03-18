package com.example.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter; // Changed from @Data
import lombok.NoArgsConstructor;
import lombok.Setter; // Changed from @Data
import lombok.ToString; // Added for better control
import lombok.EqualsAndHashCode; // Added for better control
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "products") // Exclude products to prevent potential infinite loops in toString
@EqualsAndHashCode(of = "id") // Use only id for equals and hashCode for JPA entities
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Định nghĩa mối quan hệ một-nhiều với Product
    // Changed CascadeType: Removed CascadeType.ALL to prevent accidental deletion of products
    // when a category is deleted. Deletion logic should be handled in the service layer.
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Product> products;
}
