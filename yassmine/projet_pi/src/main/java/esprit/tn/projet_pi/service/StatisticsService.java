package esprit.tn.projet_pi.service;

import esprit.tn.projet_pi.dto.DepositStatsDTO;
import esprit.tn.projet_pi.repository.SavingsDepositRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    @Autowired
    private SavingsDepositRepository depositRepository;

    public DepositStatsDTO getGlobalDepositStats() {
        long total         = depositRepository.count();
        double totalAmount = orZero(depositRepository.findTotalAmount());
        double avgAmount   = orZero(depositRepository.findAverageAmount());
        double maxAmount   = orZero(depositRepository.findMaxAmount());
        double minAmount   = orZero(depositRepository.findMinAmount());
        return new DepositStatsDTO(total, totalAmount, avgAmount, maxAmount, minAmount);
    }

    public DepositStatsDTO getDepositStatsByGoal(Long goalId) {
        long total         = orZeroLong(depositRepository.findCountByGoal(goalId));
        double totalAmount = orZero(depositRepository.findTotalAmountByGoal(goalId));
        double avgAmount   = orZero(depositRepository.findAverageAmountByGoal(goalId));
        double maxAmount   = orZero(depositRepository.findMaxAmountByGoal(goalId));
        double minAmount   = orZero(depositRepository.findMinAmountByGoal(goalId));
        return new DepositStatsDTO(total, totalAmount, avgAmount, maxAmount, minAmount);
    }

    private double orZero(Double val) { return val != null ? val : 0.0; }
    private long orZeroLong(Long val) { return val != null ? val : 0L; }
}