package com.esprit.helma_backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.esprit.helma_backend.entities.UserCategoryUsage;

public interface UserCategoryUsageRepository extends JpaRepository<UserCategoryUsage, Long> {
    List<UserCategoryUsage> findByUserIdOrderByUsageCountDesc(Long userId);
    Optional<UserCategoryUsage> findByUserIdAndCategory(Long userId, String category);
}