package esprit.tn.projet_pi.service;

import esprit.tn.projet_pi.entity.*;
import esprit.tn.projet_pi.repository.SavingsDepositRepository;
import esprit.tn.projet_pi.repository.SavingsGoalRepository;
import esprit.tn.projet_pi.repository.VoucherRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsDepositService {

    private final SavingsGoalRepository goalRepository;
    private final SavingsDepositRepository depositRepository;
    private final VoucherRepository voucherRepository;
    public VoucherCodeGenerator codeGenerator;

    public SavingsDepositService(SavingsGoalRepository goalRepository,
                                 SavingsDepositRepository depositRepository,
                                 VoucherRepository voucherRepository) {
        this.goalRepository = goalRepository;
        this.depositRepository = depositRepository;
        this.voucherRepository = voucherRepository;
        this.codeGenerator = new VoucherCodeGenerator();
    }

    public SavingsDeposit addDeposit(Long goalId, double amount){

        // 1) get goal
        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
        if(goal.getStatus() == GoalStatus.ACHIEVED){
            throw new RuntimeException("Goal already achieved. No more deposits allowed.");
        }

        // 2) create deposit
        SavingsDeposit deposit = new SavingsDeposit();
        deposit.setAmount(amount);
        deposit.setDateDeposit(LocalDate.now());
        deposit.setSavingsGoal(goal);

        depositRepository.save(deposit);

        // 3) update goal amount
        double newAmount = goal.getCurrentAmount() + amount;

        if(newAmount >= goal.getTargetAmount()){
            newAmount = goal.getTargetAmount();
        }

        goal.setCurrentAmount(newAmount);

        // 4) check achievement
        if(goal.getCurrentAmount() >= goal.getTargetAmount()
                && goal.getStatus() != GoalStatus.ACHIEVED){

            goal.setStatus(GoalStatus.ACHIEVED);

            // create voucher ONLY if not exists
            if(voucherRepository.findBySavingsGoalId(goalId).isEmpty()){

                Voucher voucher = new Voucher();
                voucher.setValue(goal.getTargetAmount() * 0.05); // 5% reward
                voucher.setCreationDate(LocalDate.now());
                voucher.setDeadline(LocalDate.now().plusMonths(1));
                voucher.setStatus(VoucherStatus.DISPONIBLE);
                voucher.setSavingsGoal(goal);

                voucher.setClaimed(false);
                voucher.setCode(codeGenerator.generateCode());
                voucherRepository.save(voucher);
            }
        }

        goalRepository.save(goal);

        return deposit;
    }



    public List<SavingsDeposit> getDepositsByUser(Long userId){
        return depositRepository.findBySavingsGoalUserId(userId);
    }
    public List<SavingsDeposit> getDepositsByGoal(Long goalId){
        return depositRepository.findBySavingsGoalId(goalId);
    }
    @Transactional
    public SavingsDeposit updateDeposit(Long depositId, double newAmount){

        SavingsDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        SavingsGoal goal = deposit.getSavingsGoal();

        double oldAmount = deposit.getAmount();

        // remove old deposit value
        goal.setCurrentAmount(goal.getCurrentAmount() - oldAmount);

        // apply new value
        deposit.setAmount(newAmount);
        goal.setCurrentAmount(goal.getCurrentAmount() + newAmount);

        // prevent overflow
        if(goal.getCurrentAmount() >= goal.getTargetAmount()){
            goal.setCurrentAmount(goal.getTargetAmount());
            goal.setStatus(GoalStatus.ACHIEVED);
        }else{
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        goalRepository.save(goal);
        return depositRepository.save(deposit);
    }
    @Transactional
    public void deleteDeposit(Long depositId){

        SavingsDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        SavingsGoal goal = deposit.getSavingsGoal();

        // subtract from goal balance
        goal.setCurrentAmount(goal.getCurrentAmount() - deposit.getAmount());

        if(goal.getCurrentAmount() < goal.getTargetAmount()){
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        goalRepository.save(goal);
        depositRepository.delete(deposit);
    }

}
