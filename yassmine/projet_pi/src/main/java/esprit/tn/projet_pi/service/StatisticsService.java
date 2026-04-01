package esprit.tn.projet_pi.service;

import esprit.tn.projet_pi.dto.DepositStatsDTO;
import esprit.tn.projet_pi.dto.GoalPredictionDTO;
import esprit.tn.projet_pi.entity.SavingsGoal;
import esprit.tn.projet_pi.repository.SavingsDepositRepository;
import esprit.tn.projet_pi.repository.SavingsGoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class StatisticsService {

    @Autowired
    private SavingsDepositRepository depositRepository;

    @Autowired
    private SavingsGoalRepository goalRepository;

    // ====== STATISTICS ======
    public DepositStatsDTO getGlobalDepositStats() {
        long total = depositRepository.count();
        double totalAmount = orZero(depositRepository.findTotalAmount());
        double avgAmount = orZero(depositRepository.findAverageAmount());
        double maxAmount = orZero(depositRepository.findMaxAmount());
        double minAmount = orZero(depositRepository.findMinAmount());
        return new DepositStatsDTO(total, totalAmount, avgAmount, maxAmount, minAmount);
    }

    public DepositStatsDTO getDepositStatsByGoal(Long goalId) {
        long total = orZeroLong(depositRepository.findCountByGoal(goalId));
        double totalAmount = orZero(depositRepository.findTotalAmountByGoal(goalId));
        double avgAmount = orZero(depositRepository.findAverageAmountByGoal(goalId));
        double maxAmount = orZero(depositRepository.findMaxAmountByGoal(goalId));
        double minAmount = orZero(depositRepository.findMinAmountByGoal(goalId));
        return new DepositStatsDTO(total, totalAmount, avgAmount, maxAmount, minAmount);
    }

    // ====== PREDICTION ======
    public GoalPredictionDTO predictGoalAchievement(Long goalId) {

        //  Récupérer l'objectif depuis la base de données
        // Si l'id n'existe pas → erreur
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        //  Calcul du montant restant à épargner
        // remaining = objectif - montant actuel
        double remaining = goal.getTargetAmount() - goal.getCurrentAmount();

        //  Calcul du nombre de jours restants avant la deadline
        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getDeadline());

        //  Récupérer le total des dépôts effectués pour cet objectif
        double totalAmount = orZero(depositRepository.findTotalAmountByGoal(goalId));

        // Calcul du nombre de jours depuis la création de l'objectif
        long daysSinceCreation = ChronoUnit.DAYS.between(goal.getCreationDate(), LocalDate.now());


        // = combien l'utilisateur économise par jour en moyenne
        double actualPerDay = daysSinceCreation > 0 ? totalAmount / daysSinceCreation : 0;


        // = combien il doit économiser par jour
        double requiredPerDay = daysRemaining > 0 ? remaining / daysRemaining : 0;


        String prediction;
        String message;

        //  Cas 1 : objectif déjà atteint
        if (goal.getStatus().name().equals("ACHIEVED")) {
            prediction = "ACHIEVED";
            message = "Félicitations ! Objectif déjà atteint !";

            //  Cas 2 : deadline dépassée
        } else if (daysRemaining <= 0) {
            prediction = "FAILED";
            message = "Deadline dépassée !";

            //  Cas 3 : utilisateur sur la bonne voie
            // son rythme actuel ≥ rythme nécessaire
        } else if (actualPerDay >= requiredPerDay) {
            prediction = "WILL_ACHIEVE";
            message = "Vous êtes sur la bonne voie !";

            //  Cas 4 : utilisateur en retard
        } else {
            prediction = "WILL_NOT_ACHIEVE";

            // Message dynamique : combien il doit augmenter par jour
            message = "Augmentez vos dépôts de " +
                    String.format("%.2f", requiredPerDay - actualPerDay) + " TND/jour.";
        }

        // Retourner le résultat sous forme de DTO (JSON)
        return new GoalPredictionDTO(
                goal.getTitle(),
                goal.getTargetAmount(),       // montant cible
                goal.getCurrentAmount(),      // montant actuel
                remaining,                   // montant restant
                daysRemaining,               // jours restants
                requiredPerDay,              // effort nécessaire
                actualPerDay,                // effort réel
                prediction,                  // décision finale
                message
        );
    }

    private double orZero(Double val) { return val != null ? val : 0.0; }
    private long orZeroLong(Long val) { return val != null ? val : 0L; }
}