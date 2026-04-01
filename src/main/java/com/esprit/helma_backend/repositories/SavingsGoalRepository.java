package com.esprit.helma_backend.repositories;

import com.esprit.helma_backend.entities.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(Long userId);

    int countByUserIdAndCompletedTrue(Long userId);
    List<SavingsGoal> findByUserIdAndCompletedFalseOrderByDeadlineAsc(Long userId);
}