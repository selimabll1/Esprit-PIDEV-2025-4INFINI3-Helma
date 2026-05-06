package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.RiskCaseDto;
import com.esprit.helma_backend.entities.RiskCase;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.RiskCaseRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RiskCaseService {

    private final RiskCaseRepository riskRepo;
    private final UserRepository userRepo;
    private final AuditLogService auditLogService;
    private final RiskNotificationService riskNotificationService;

    public RiskCaseService(RiskCaseRepository riskRepo,
                           UserRepository userRepo,
                           AuditLogService auditLogService,
                           RiskNotificationService riskNotificationService) {
        this.riskRepo = riskRepo;
        this.userRepo = userRepo;
        this.auditLogService = auditLogService;
        this.riskNotificationService = riskNotificationService;
    }

    private static RiskCaseDto.Response toResponse(RiskCase rc) {
        return new RiskCaseDto.Response(
                rc.getId(),
                rc.getUser().getId(),
                rc.getRiskLevel(),
                rc.getAssignedAdmin() != null ? rc.getAssignedAdmin().getId() : null,
                rc.getStatus() != null ? rc.getStatus().name() : null,
                rc.getDetectedAt()
        );
    }

    public RiskCaseDto.Response upsertOpenCase(Long userId, int riskLevel, List<String> reasons) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        RiskCase rc = riskRepo
                .findTopByUserIdAndStatusOrderByDetectedAtDesc(userId, RiskCase.Status.OPEN)
                .orElse(null);

        boolean isNew = rc == null;
        int previousLevel = isNew ? 0 : rc.getRiskLevel();

        if (isNew) {
            rc = RiskCase.builder()
                    .user(user)
                    .riskLevel(riskLevel)
                    .status(RiskCase.Status.OPEN)
                    .detectedAt(Instant.now())
                    .build();
        } else {
            rc.setRiskLevel(riskLevel);
            rc.setDetectedAt(Instant.now());
        }

        RiskCase saved = riskRepo.save(rc);

        auditLogService.log(
                userId,
                "RISK_CASE_UPSERTED",
                "RiskCase",
                saved.getId(),
                "riskLevel=" + saved.getRiskLevel()
                        + ",status=" + saved.getStatus()
                        + ",reasons=" + String.join("|", reasons)
        );

        // Always notify — the risk engine already decided this transaction is trigger-worthy.
        // Suppressing on "not escalating" hides valid alerts when a case already exists.
        String notifyMessage = isNew
                ? "Risk detected. Score: " + riskLevel + ". Reasons: " + String.join(", ", reasons)
                : "Risk updated. Score: " + riskLevel + " (was " + previousLevel + "). Reasons: " + String.join(", ", reasons);
        riskNotificationService.sendRiskAlert(userId, notifyMessage, riskLevel, reasons);

        return toResponse(saved);
    }

    public RiskCaseDto.Response create(RiskCaseDto.Create req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User admin = null;
        if (req.assignedAdminId() != null) {
            admin = userRepo.findById(req.assignedAdminId())
                    .orElseThrow(() -> new IllegalArgumentException("Assigned admin not found"));
        }

        RiskCase.Status st = (req.status() == null || req.status().isBlank())
                ? RiskCase.Status.OPEN
                : RiskCase.Status.valueOf(req.status().toUpperCase());

        if (st == RiskCase.Status.OPEN) {
            RiskCaseDto.Response updated = upsertOpenCase(user.getId(), req.riskLevel(), List.of("MANUAL_CREATE"));
            RiskCase entity = riskRepo.findById(updated.id()).orElseThrow();
            entity.setAssignedAdmin(admin);
            return toResponse(riskRepo.save(entity));
        }

        RiskCase rc = RiskCase.builder()
                .user(user)
                .riskLevel(req.riskLevel())
                .assignedAdmin(admin)
                .status(st)
                .detectedAt(Instant.now())
                .build();

        return toResponse(riskRepo.save(rc));
    }

    @Transactional(readOnly = true)
    public RiskCaseDto.Response getById(Long id) {
        RiskCase rc = riskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RiskCase not found"));
        return toResponse(rc);
    }

    @Transactional(readOnly = true)
    public List<RiskCaseDto.Response> getAll() {
        return riskRepo.findAll().stream().map(RiskCaseService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RiskCaseDto.Response> getByUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return riskRepo.findByUser(user).stream().map(RiskCaseService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RiskCaseDto.Response> getByAssignedAdmin(Long adminId) {
        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        return riskRepo.findByAssignedAdmin(admin).stream().map(RiskCaseService::toResponse).toList();
    }

    public RiskCaseDto.Response update(Long id, RiskCaseDto.Update req) {
        RiskCase rc = riskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RiskCase not found"));

        int previousLevel = rc.getRiskLevel();
        Long userId = rc.getUser().getId();

        rc.setRiskLevel(req.riskLevel());

        if (req.assignedAdminId() == null) {
            rc.setAssignedAdmin(null);
        } else {
            User admin = userRepo.findById(req.assignedAdminId())
                    .orElseThrow(() -> new IllegalArgumentException("Assigned admin not found"));
            rc.setAssignedAdmin(admin);
        }

        if (req.status() != null && !req.status().isBlank()) {
            rc.setStatus(RiskCase.Status.valueOf(req.status().toUpperCase()));
        }

        rc.setDetectedAt(Instant.now());
        RiskCaseDto.Response response = toResponse(riskRepo.save(rc));

        if (req.riskLevel() > previousLevel) {
            riskNotificationService.sendRiskAlert(userId,
                    "Risk level escalated from " + previousLevel + " to " + req.riskLevel(),
                    req.riskLevel(), List.of("RISK_TRIGGERED"));
        }

        return response;
    }

    public void delete(Long id) {
        if (!riskRepo.existsById(id)) {
            throw new IllegalArgumentException("RiskCase not found");
        }
        riskRepo.deleteById(id);
    }
}