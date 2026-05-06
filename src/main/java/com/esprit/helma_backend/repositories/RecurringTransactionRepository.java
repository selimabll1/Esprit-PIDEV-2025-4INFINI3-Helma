package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserId(Long userId);

    List<RecurringTransaction> findAllByActiveTrueAndNextDateLessThanEqual(LocalDate date);
}
