package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.IncomeStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeStatementRepository extends JpaRepository<IncomeStatement, Long> {

    Optional<IncomeStatement> findByUserIdAndMonthStart(Long userId, LocalDate monthStart);

    List<IncomeStatement> findByUserIdOrderByMonthStartAsc(Long userId);

    Optional<IncomeStatement> findTopByUserIdOrderByMonthStartDesc(Long userId);
}