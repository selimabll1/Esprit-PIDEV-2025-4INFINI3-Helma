package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.SavingsGoalDto;
import com.esprit.helma_backend.entities.SavingsGoal;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.SavingsGoalRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class SavingsGoalService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final SavingsGoalRepository savingsGoalRepo;
    private final UserRepository userRepo;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepo,
                              UserRepository userRepo) {
        this.savingsGoalRepo = savingsGoalRepo;
        this.userRepo = userRepo;
    }

    public SavingsGoalDto.Response create(SavingsGoalDto.Create req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .name(req.name())
                .targetAmount(nz(req.targetAmount()))
                .currentAmount(ZERO)
                .deadline(req.deadline())
                .completed(false)
                .build();

        goal.setWeeklyTarget(computeWeeklyTarget(
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getDeadline()
        ));
        goal.setCompleted(isCompleted(
                goal.getCurrentAmount(),
                goal.getTargetAmount(),
                goal.getDeadline()
        ));

        if (Boolean.TRUE.equals(goal.getCompleted())) {
            goal.setWeeklyTarget(ZERO);
        }

        return toResponse(savingsGoalRepo.save(goal));
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalDto.Response> getByUser(Long userId) {
        return savingsGoalRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SavingsGoalDto.Response addProgress(Long goalId, BigDecimal amount) {
        SavingsGoal goal = savingsGoalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("SavingsGoal not found"));

        BigDecimal progress = nz(amount);
        if (progress.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Progress amount must be >= 0");
        }

        goal.setCurrentAmount(nz(goal.getCurrentAmount()).add(progress));

        boolean completed = isCompleted(
                goal.getCurrentAmount(),
                goal.getTargetAmount(),
                goal.getDeadline()
        );
        goal.setCompleted(completed);

        if (completed) {
            goal.setWeeklyTarget(ZERO);
        } else {
            goal.setWeeklyTarget(computeWeeklyTarget(
                    goal.getTargetAmount(),
                    goal.getCurrentAmount(),
                    goal.getDeadline()
            ));
        }

        return toResponse(savingsGoalRepo.save(goal));
    }

    public SavingsGoalDto.Response recalculateWeeklyTarget(Long goalId) {
        SavingsGoal goal = savingsGoalRepo.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("SavingsGoal not found"));

        boolean completed = isCompleted(
                goal.getCurrentAmount(),
                goal.getTargetAmount(),
                goal.getDeadline()
        );
        goal.setCompleted(completed);

        if (completed) {
            goal.setWeeklyTarget(ZERO);
        } else {
            goal.setWeeklyTarget(computeWeeklyTarget(
                    goal.getTargetAmount(),
                    goal.getCurrentAmount(),
                    goal.getDeadline()
            ));
        }

        return toResponse(savingsGoalRepo.save(goal));
    }

    public void delete(Long goalId) {
        if (!savingsGoalRepo.existsById(goalId)) {
            throw new IllegalArgumentException("SavingsGoal not found");
        }
        savingsGoalRepo.deleteById(goalId);
    }

    private SavingsGoalDto.Response toResponse(SavingsGoal g) {
        return new SavingsGoalDto.Response(
                g.getId(),
                g.getUser().getId(),
                g.getName(),
                g.getTargetAmount(),
                g.getCurrentAmount(),
                g.getDeadline(),
                g.getWeeklyTarget(),
                g.getCompleted(),
                g.getCreatedAt()
        );
    }

    private boolean isCompleted(BigDecimal currentAmount,
                                BigDecimal targetAmount,
                                LocalDate deadline) {
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            return true;
        }
        return nz(currentAmount).compareTo(nz(targetAmount)) >= 0;
    }

    private BigDecimal computeWeeklyTarget(BigDecimal targetAmount,
                                           BigDecimal currentAmount,
                                           LocalDate deadline) {
        if (deadline == null) return ZERO;
        if (deadline.isBefore(LocalDate.now())) return ZERO;
        if (nz(currentAmount).compareTo(nz(targetAmount)) >= 0) return ZERO;

        BigDecimal remaining = nz(targetAmount).subtract(nz(currentAmount));
        if (remaining.compareTo(ZERO) <= 0) return ZERO;

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        long weeksRemaining = Math.max(1, (long) Math.ceil(daysRemaining / 7.0));

        return remaining.divide(BigDecimal.valueOf(weeksRemaining), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? ZERO : v;
    }
}