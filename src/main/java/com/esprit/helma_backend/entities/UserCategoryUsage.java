package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "user_category_usage",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
public class UserCategoryUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private int usageCount = 0;
}