package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.TrustBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrustBadgeRepository extends JpaRepository<TrustBadge, Long> {
    Optional<TrustBadge> findByUserId(Long userId);
}