package com.helma.helmabackend.repository;

 import com.helma.helmabackend.entity.SavingsGoal;
 import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    // tous les goals d'un utilisateur
    List<SavingsGoal> findByUserId(Long userId);

}
