package esprit.tn.projet_pi.repository;

import esprit.tn.projet_pi.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    // tous les goals d'un utilisateur
    List<SavingsGoal> findByUserId(Long userId);

}
