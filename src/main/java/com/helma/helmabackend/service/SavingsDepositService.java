package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.*;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.repository.SavingsDepositRepository;
import com.helma.helmabackend.repository.SavingsGoalRepository;
import com.helma.helmabackend.repository.VoucherRepository;
import com.helma.helmabackend.repository.user.UserRepository;
import com.helma.helmabackend.security.util.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsDepositService {

    private final SavingsGoalRepository goalRepository;
    private final SavingsDepositRepository depositRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final VoucherCodeGenerator codeGenerator;

    public SavingsDepositService(
            SavingsGoalRepository goalRepository,
            SavingsDepositRepository depositRepository,
            VoucherRepository voucherRepository,
            UserRepository userRepository
    ) {
        this.goalRepository = goalRepository;
        this.depositRepository = depositRepository;
        this.voucherRepository = voucherRepository;
        this.userRepository = userRepository;
        this.codeGenerator = new VoucherCodeGenerator();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.currentEmail();

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private SavingsGoal getUserGoalOrThrow(Long goalId) {
        User user = getCurrentUser();

        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (goal.getUser() == null || !goal.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return goal;
    }

    private SavingsDeposit getUserDepositOrThrow(Long depositId) {
        SavingsDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        SavingsGoal goal = deposit.getSavingsGoal();

        getUserGoalOrThrow(goal.getId());

        return deposit;
    }

    public List<SavingsDeposit> getAllDeposits() {
        User user = getCurrentUser();
        return depositRepository.findBySavingsGoalUserId(user.getId());
    }

    public SavingsDeposit getDepositById(Long id) {
        return getUserDepositOrThrow(id);
    }

    @Transactional
    public SavingsDeposit addDeposit(Long goalId, double amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        SavingsGoal goal = getUserGoalOrThrow(goalId);
        if (goal.getDeadline() != null && goal.getDeadline().isBefore(LocalDate.now())) {
            goal.setStatus(GoalStatus.EXPIRED);
            goalRepository.save(goal);

            throw new RuntimeException(
                    "This goal has expired. Deposits are no longer allowed."
            );
        }

        if (goal.getStatus() == GoalStatus.ACHIEVED) {
            throw new RuntimeException("Goal already achieved");
        }

        SavingsDeposit deposit = new SavingsDeposit();
        deposit.setAmount(amount);
        deposit.setDateDeposit(LocalDate.now());
        deposit.setSavingsGoal(goal);

        depositRepository.save(deposit);

        double newAmount = goal.getCurrentAmount() + amount;

        if (newAmount >= goal.getTargetAmount()) {
            newAmount = goal.getTargetAmount();
        }

        goal.setCurrentAmount(newAmount);

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setStatus(GoalStatus.ACHIEVED);
            createVoucherIfMissing(goal);
        }

        goalRepository.save(goal);

        return deposit;
    }

    public List<SavingsDeposit> getDepositsByUser(Long userId) {
        User user = getCurrentUser();

        if (!user.getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return depositRepository.findBySavingsGoalUserId(user.getId());
    }

    public List<SavingsDeposit> getDepositsByGoal(Long goalId) {
        SavingsGoal goal = getUserGoalOrThrow(goalId);
        return depositRepository.findBySavingsGoalId(goal.getId());
    }

    @Transactional
    public SavingsDeposit updateDeposit(Long depositId, double newAmount) {

        if (newAmount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        SavingsDeposit deposit = getUserDepositOrThrow(depositId);
        SavingsGoal goal = deposit.getSavingsGoal();

        double oldAmount = deposit.getAmount();

        goal.setCurrentAmount(goal.getCurrentAmount() - oldAmount);

        deposit.setAmount(newAmount);

        goal.setCurrentAmount(goal.getCurrentAmount() + newAmount);

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {

            goal.setCurrentAmount(goal.getTargetAmount());
            goal.setStatus(GoalStatus.ACHIEVED);
            createVoucherIfMissing(goal);

        } else {

            if (goal.getDeadline() != null && goal.getDeadline().isBefore(LocalDate.now())) {
                goal.setStatus(GoalStatus.EXPIRED);
            } else {
                goal.setStatus(GoalStatus.IN_PROGRESS);
            }
        }

        goalRepository.save(goal);

        return depositRepository.save(deposit);
    }

    @Transactional
    public void deleteDeposit(Long depositId) {

        SavingsDeposit deposit = getUserDepositOrThrow(depositId);
        SavingsGoal goal = deposit.getSavingsGoal();

        double newAmount = goal.getCurrentAmount() - deposit.getAmount();

        if (newAmount < 0) {
            newAmount = 0;
        }

        goal.setCurrentAmount(newAmount);

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setStatus(GoalStatus.ACHIEVED);
        } else if (goal.getDeadline() != null && goal.getDeadline().isBefore(LocalDate.now())) {
            goal.setStatus(GoalStatus.EXPIRED);
        } else {
            goal.setStatus(GoalStatus.IN_PROGRESS);
        }

        goalRepository.save(goal);
        depositRepository.delete(deposit);
    }

    private void createVoucherIfMissing(SavingsGoal goal) {

        if (voucherRepository.findBySavingsGoalId(goal.getId()).isEmpty()) {

            Voucher voucher = new Voucher();

            voucher.setValue(goal.getTargetAmount() * 0.05);
            voucher.setCreationDate(LocalDate.now());
            voucher.setDeadline(LocalDate.now().plusMonths(1));
            voucher.setStatus(VoucherStatus.DISPONIBLE);
            voucher.setClaimed(false);
            voucher.setCode(codeGenerator.generateCode());
            voucher.setSavingsGoal(goal);

            voucherRepository.save(voucher);
        }
    }
}