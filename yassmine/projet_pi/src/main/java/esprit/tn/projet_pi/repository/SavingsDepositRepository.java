package esprit.tn.projet_pi.repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import esprit.tn.projet_pi.entity.SavingsDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingsDepositRepository extends JpaRepository<SavingsDeposit, Long> {

    List<SavingsDeposit> findBySavingsGoalId(Long goalId);
    List<SavingsDeposit> findBySavingsGoalUserId(Long userId);
    @Query("SELECT SUM(d.amount) FROM SavingsDeposit d")
    Double findTotalAmount();

    @Query("SELECT AVG(d.amount) FROM SavingsDeposit d")
    Double findAverageAmount();

    @Query("SELECT MAX(d.amount) FROM SavingsDeposit d")
    Double findMaxAmount();

    @Query("SELECT MIN(d.amount) FROM SavingsDeposit d")
    Double findMinAmount();

    @Query("SELECT SUM(d.amount) FROM SavingsDeposit d WHERE d.savingsGoal.id = :goalId")
    Double findTotalAmountByGoal(@Param("goalId") Long goalId);

    @Query("SELECT COUNT(d) FROM SavingsDeposit d WHERE d.savingsGoal.id = :goalId")
    Long findCountByGoal(@Param("goalId") Long goalId);

    @Query("SELECT AVG(d.amount) FROM SavingsDeposit d WHERE d.savingsGoal.id = :goalId")
    Double findAverageAmountByGoal(@Param("goalId") Long goalId);

    @Query("SELECT MAX(d.amount) FROM SavingsDeposit d WHERE d.savingsGoal.id = :goalId")
    Double findMaxAmountByGoal(@Param("goalId") Long goalId);

    @Query("SELECT MIN(d.amount) FROM SavingsDeposit d WHERE d.savingsGoal.id = :goalId")
    Double findMinAmountByGoal(@Param("goalId") Long goalId);

}
