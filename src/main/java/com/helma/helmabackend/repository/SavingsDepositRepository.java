package com.helma.helmabackend.repository;

 import com.helma.helmabackend.entity.SavingsDeposit;
 import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SavingsDepositRepository extends JpaRepository<SavingsDeposit, Long> {

    void deleteBySavingsGoalId(Long goalId);  // ← ajoutez cette ligne

    List<SavingsDeposit> findBySavingsGoalId(Long goalId);
    List<SavingsDeposit> findBySavingsGoalUserId(Long userId);
    @Query("""
        SELECT d
        FROM SavingsDeposit d
        JOIN FETCH d.savingsGoal g
        JOIN FETCH g.user u
        ORDER BY d.dateDeposit DESC, d.id DESC
    """)
    List<SavingsDeposit> findAllWithUserAndGoal();

}
