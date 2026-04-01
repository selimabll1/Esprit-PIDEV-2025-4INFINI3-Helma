package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.RiskCase;
import com.esprit.helma_backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RiskCaseRepository extends JpaRepository<RiskCase, Long> {

    List<RiskCase> findByUser(User user);

    List<RiskCase> findByAssignedAdmin(User admin);

    Optional<RiskCase> findTopByUserIdAndStatusOrderByDetectedAtDesc(Long userId, RiskCase.Status status);

    boolean existsByUserIdAndStatus(Long userId, RiskCase.Status status);

    long countByUserIdAndStatus(Long userId, RiskCase.Status status);

    default long countOpenByUserId(Long userId) {
        return countByUserIdAndStatus(userId, RiskCase.Status.OPEN);
    }

    @Query("""
            select coalesce(max(r.riskLevel), 0)
            from RiskCase r
            where r.user.id = :userId
              and r.status = com.esprit.helma_backend.entities.RiskCase.Status.OPEN
              and r.detectedAt >= :since
           """)
    int maxOpenRiskLevelLast30Days(@Param("userId") Long userId,
                                   @Param("since") Instant since);
}