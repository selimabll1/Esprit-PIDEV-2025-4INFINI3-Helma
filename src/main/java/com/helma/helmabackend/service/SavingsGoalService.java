package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.GoalStatus;
import com.helma.helmabackend.entity.SavingsGoal;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.repository.SavingsDepositRepository;
import com.helma.helmabackend.repository.SavingsGoalRepository;
import com.helma.helmabackend.repository.user.UserRepository;
import com.helma.helmabackend.security.util.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import com.helma.helmabackend.dto.savings.RiskAnalysisDTO;
import com.helma.helmabackend.entity.SavingsDeposit;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final SavingsDepositRepository depositRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository,
                              UserRepository userRepository,
                              SavingsDepositRepository depositRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
    }

    // CREATE
    public SavingsGoal createGoal(SavingsGoal goal) {
        String email = SecurityUtils.currentEmail();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        goal.setUser(user);
        goal.setCreationDate(LocalDate.now());
        goal.setCurrentAmount(0);
        goal.setStatus(GoalStatus.IN_PROGRESS);
        return goalRepository.save(goal);
    }

    // READ
    public List<SavingsGoal> getUserGoals() {
        String email = SecurityUtils.currentEmail();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return goalRepository.findByUserId(user.getId());
    }

    // DELETE
    @Transactional
    public void deleteGoal(Long id) {
        depositRepository.deleteBySavingsGoalId(id); // ← supprime dépôts d'abord
        goalRepository.deleteById(id);               // ← puis supprime le goal
    }

    // GET ALL
    public List<SavingsGoal> getAllGoals() {
        return goalRepository.findAll();
    }

    // UPDATE
    public SavingsGoal updateGoal(Long id, SavingsGoal newGoal) {
        SavingsGoal goal = goalRepository.findById(id).orElseThrow();
        goal.setTitle(newGoal.getTitle());
        goal.setTargetAmount(newGoal.getTargetAmount());
        goal.setDeadline(newGoal.getDeadline());
        return goalRepository.save(goal);
    }

    // GET ONE
    public SavingsGoal getGoalById(Long id) {
        return goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
    }


       public List<RiskAnalysisDTO> getRiskAnalysis() {
        List<SavingsGoal> goals = goalRepository.findAll();
        List<RiskAnalysisDTO> riskAnalysisList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (SavingsGoal goal : goals) {
            try {
                String riskLevel = "LOW";
                String reason = "Healthy progression";
                String recommendation = "Continue saving";

                double target = goal.getTargetAmount();
                double current = goal.getCurrentAmount();
                double progress = target > 0 ? (current / target) * 100 : 0;
                
                LocalDate deadline = goal.getDeadline();
                long daysToDeadline = (deadline != null) ? ChronoUnit.DAYS.between(today, deadline) : 365;

                // 1. High Risk Verification
                if (current > target && target > 0) {
                    riskLevel = "HIGH";
                    reason = "Saved amount exceeds target";
                    recommendation = "Review goal target";
                } else if (progress < 30 && deadline != null && daysToDeadline < 15) {
                    riskLevel = "HIGH";
                    reason = "High risk of missing deadline";
                    recommendation = "Increase deposit frequency";
                }
                // 2. Inactivity Verification
                else {
                    LocalDate lastDepositDate = goal.getCreationDate();
                    List<SavingsDeposit> deposits = goal.getDeposits();
                    
                    if (deposits != null && !deposits.isEmpty()) {
                        for (SavingsDeposit d : deposits) {
                            if (d.getDateDeposit() != null && (lastDepositDate == null || d.getDateDeposit().isAfter(lastDepositDate))) {
                                lastDepositDate = d.getDateDeposit();
                            }
                        }
                    }

                    long daysSinceLastDeposit = (lastDepositDate != null) ? ChronoUnit.DAYS.between(lastDepositDate, today) : 0;

                    if (daysSinceLastDeposit >= 30) {
                        riskLevel = "INACTIVE";
                        reason = "No deposits for over 30 days";
                        recommendation = "Contact the user";
                    }
                    else if (progress >= 30 && progress <= 70) {
                        riskLevel = "MEDIUM";
                        reason = "Moderate saving pace";
                        recommendation = "Notify the user";
                    } else if (daysSinceLastDeposit > 15) {
                        riskLevel = "MEDIUM";
                        reason = "Irregular deposits";
                        recommendation = "Send a reminder";
                    }
                }

                String userName = "Unknown User";
                if (goal.getUser() != null) {
                    if (goal.getUser().getProfile() != null) {
                        userName = goal.getUser().getProfile().getFirstName() + " " + goal.getUser().getProfile().getLastName();
                    } else {
                        userName = goal.getUser().getEmail();
                    }
                }

                riskAnalysisList.add(new RiskAnalysisDTO(
                        goal.getId(),
                        goal.getTitle() != null ? goal.getTitle() : "Untitled",
                        userName,
                        riskLevel,
                        reason,
                        recommendation
                ));
            } catch (Exception e) {
                // Skip failed analysis for a single goal instead of breaking the whole list
                System.err.println("Error analyzing goal " + goal.getId() + ": " + e.getMessage());
            }
        }

        return riskAnalysisList;
    }
}