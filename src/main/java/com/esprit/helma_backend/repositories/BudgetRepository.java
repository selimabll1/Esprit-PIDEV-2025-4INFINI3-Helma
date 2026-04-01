package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdOrderByMonthStartDesc(Long userId);

    List<Budget> findByUserIdAndMonthStartOrderByCategoryAsc(Long userId, LocalDate monthStart);

    Optional<Budget> findByUserIdAndMonthStartAndCategory(Long userId, LocalDate monthStart, String category);
}