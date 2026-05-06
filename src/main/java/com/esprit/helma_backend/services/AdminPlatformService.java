package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.AdminPlatformDto;
import com.esprit.helma_backend.entities.RiskCase;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.RiskCaseRepository;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminPlatformService {

    private final UserRepository userRepo;
    private final RiskCaseRepository riskCaseRepo;
    private final TransactionRepository transactionRepo;

    public AdminPlatformService(UserRepository userRepo,
                                 RiskCaseRepository riskCaseRepo,
                                 TransactionRepository transactionRepo) {
        this.userRepo = userRepo;
        this.riskCaseRepo = riskCaseRepo;
        this.transactionRepo = transactionRepo;
    }

    @Transactional(readOnly = true)
    public AdminPlatformDto.PlatformStats getStats() {
        long totalUsers = userRepo.count();
        long totalEntrepreneurs = userRepo.countByRole(User.Role.ENTREPRENEUR);
        long totalBasicUsers = userRepo.countByRole(User.Role.USER);
        long totalAdmins = userRepo.countByRole(User.Role.ADMIN);
        long openRiskCases = riskCaseRepo.findAll().stream()
                .filter(rc -> rc.getStatus() == RiskCase.Status.OPEN)
                .count();
        long resolvedRiskCases = riskCaseRepo.findAll().stream()
                .filter(rc -> rc.getStatus() == RiskCase.Status.RESOLVED)
                .count();
        long totalTransactions = transactionRepo.count();

        return new AdminPlatformDto.PlatformStats(
                totalUsers,
                totalEntrepreneurs,
                totalBasicUsers,
                totalAdmins,
                openRiskCases,
                resolvedRiskCases,
                totalTransactions
        );
    }

    @Transactional(readOnly = true)
    public List<AdminPlatformDto.UserRow> getAllUsers() {
        return userRepo.findAll().stream()
                .map(u -> new AdminPlatformDto.UserRow(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole(),
                        u.getIsEntrepreneur()
                ))
                .toList();
    }

    public AdminPlatformDto.UserRow changeRole(Long userId, User.Role newRole) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        u.setRole(newRole);
        if (newRole == User.Role.ENTREPRENEUR) {
            u.setIsEntrepreneur(true);
        } else if (newRole == User.Role.USER) {
            u.setIsEntrepreneur(false);
        }
        userRepo.save(u);
        return new AdminPlatformDto.UserRow(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getIsEntrepreneur());
    }

    public void deleteUser(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        userRepo.deleteById(userId);
    }
}
