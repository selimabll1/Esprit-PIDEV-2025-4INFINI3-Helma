package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.CashFlowSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashFlowSummaryRepository extends JpaRepository<CashFlowSummary, Long> {

    Optional<CashFlowSummary> findByUserIdAndMonthStart(Long userId, LocalDate monthStart);

    List<CashFlowSummary> findByUserIdOrderByMonthStartAsc(Long userId);

    Optional<CashFlowSummary> findTopByUserIdAndMonthStartLessThanOrderByMonthStartDesc(
            Long userId,
            LocalDate monthStart
    );

    default Optional<CashFlowSummary> findPreviousMonth(Long userId, LocalDate monthStart) {
        return findTopByUserIdAndMonthStartLessThanOrderByMonthStartDesc(userId, monthStart);
    }

    @Query("""
        SELECT COUNT(c)
        FROM CashFlowSummary c
        WHERE c.user.id = :userId
    """)
    long countMonthsOfHistoryByUserId(@Param("userId") Long userId);

    Optional<CashFlowSummary> findTopByUserIdOrderByMonthStartDesc(Long userId);
}