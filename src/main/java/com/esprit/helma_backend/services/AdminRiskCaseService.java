package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.AdminRiskCaseDto;
import com.esprit.helma_backend.dto.AdminRiskCaseDto.RiskSeverity;
import com.esprit.helma_backend.entities.RiskCase;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.AuditLogRepository;
import com.esprit.helma_backend.repositories.RiskCaseRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class AdminRiskCaseService {

    private final RiskCaseRepository riskRepo;
    private final UserRepository userRepo;
    private final AuditLogRepository auditLogRepo;
    private final AuditLogService auditLogService;

    public AdminRiskCaseService(RiskCaseRepository riskRepo,
                                UserRepository userRepo,
                                AuditLogRepository auditLogRepo,
                                AuditLogService auditLogService) {
        this.riskRepo = riskRepo;
        this.userRepo = userRepo;
        this.auditLogRepo = auditLogRepo;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public AdminRiskCaseDto.DashboardResponse getDashboard(String statusFilter) {
        List<RiskCase> cases = resolveFilter(statusFilter).stream()
                .sorted(Comparator
                        .comparing((RiskCase rc) -> rc.getStatus() != RiskCase.Status.OPEN)
                        .thenComparing(RiskCase::getRiskLevel, Comparator.reverseOrder())
                        .thenComparing(RiskCase::getDetectedAt, Comparator.reverseOrder()))
                .toList();

        List<AdminRiskCaseDto.CaseResponse> enriched = cases.stream()
                .map(this::enrich)
                .toList();

        List<RiskCase> all = riskRepo.findAll();
        AdminRiskCaseDto.StatsResponse stats = buildStats(all);

        return new AdminRiskCaseDto.DashboardResponse(stats, enriched);
    }

    @Transactional(readOnly = true)
    public AdminRiskCaseDto.CaseResponse getById(Long id) {
        return enrich(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AdminRiskCaseDto.AdminOption> listAdmins() {
        return userRepo.findAll().stream()
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> new AdminRiskCaseDto.AdminOption(u.getId(), u.getFullName(), u.getEmail()))
                .toList();
    }

    public AdminRiskCaseDto.CaseResponse resolve(Long id,
                                                 AdminRiskCaseDto.ResolveRequest req,
                                                 Long actingAdminId) {
        RiskCase rc = findOrThrow(id);

        if (rc.getStatus() == RiskCase.Status.RESOLVED) {
            throw new IllegalStateException("Case " + id + " is already resolved.");
        }

        rc.setStatus(RiskCase.Status.RESOLVED);
        RiskCase saved = riskRepo.save(rc);

        auditLogService.log(
                actingAdminId,
                "RISK_CASE_RESOLVED",
                "RiskCase",
                saved.getId(),
                "resolvedBy=" + actingAdminId
                        + ",note=" + (req != null && req.resolutionNote() != null ? req.resolutionNote() : "")
        );

        return enrich(saved);
    }

    public AdminRiskCaseDto.CaseResponse assign(Long id,
                                                AdminRiskCaseDto.AssignRequest req,
                                                Long actingAdminId) {
        if (req == null || req.adminId() == null) {
            throw new IllegalArgumentException("adminId is required");
        }

        RiskCase rc = findOrThrow(id);

        User admin = userRepo.findById(req.adminId())
                .filter(u -> u.getRole() == User.Role.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found or user is not ADMIN: " + req.adminId()));

        rc.setAssignedAdmin(admin);
        RiskCase saved = riskRepo.save(rc);

        auditLogService.log(
                actingAdminId,
                "RISK_CASE_ASSIGNED",
                "RiskCase",
                saved.getId(),
                "assignedTo=" + req.adminId() + ",by=" + actingAdminId
        );

        return enrich(saved);
    }

    public AdminRiskCaseDto.CaseResponse reopen(Long id, Long actingAdminId) {
        RiskCase rc = findOrThrow(id);

        if (rc.getStatus() == RiskCase.Status.OPEN) {
            throw new IllegalStateException("Case " + id + " is already open.");
        }

        rc.setStatus(RiskCase.Status.OPEN);
        rc.setDetectedAt(Instant.now());
        RiskCase saved = riskRepo.save(rc);

        auditLogService.log(
                actingAdminId,
                "RISK_CASE_REOPENED",
                "RiskCase",
                saved.getId(),
                "reopenedBy=" + actingAdminId
        );

        return enrich(saved);
    }

    private List<RiskCase> resolveFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            return riskRepo.findAll();
        }

        RiskCase.Status status = RiskCase.Status.valueOf(statusFilter.toUpperCase());
        return riskRepo.findAll().stream()
                .filter(rc -> rc.getStatus() == status)
                .toList();
    }

    private AdminRiskCaseDto.CaseResponse enrich(RiskCase rc) {
        String reasons = auditLogRepo.findByUserIdOrderByCreatedAtDesc(rc.getUser().getId())
                .stream()
                .filter(l -> "RISK_CASE_UPSERTED".equals(l.getAction()) && rc.getId().equals(l.getEntityId()))
                .findFirst()
                .map(l -> {
                    String details = l.getDetails();
                    if (details != null && details.contains("reasons=")) {
                        return details.substring(details.indexOf("reasons=") + 8).replace("|", ", ");
                    }
                    return null;
                })
                .orElse(null);

        User user = rc.getUser();
        User admin = rc.getAssignedAdmin();

        return new AdminRiskCaseDto.CaseResponse(
                rc.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getIsEntrepreneur(),
                rc.getRiskLevel(),
                RiskSeverity.of(rc.getRiskLevel()),
                admin != null ? admin.getId() : null,
                admin != null ? admin.getFullName() : null,
                rc.getStatus().name(),
                rc.getDetectedAt(),
                reasons
        );
    }

    private AdminRiskCaseDto.StatsResponse buildStats(List<RiskCase> all) {
        long totalOpen = all.stream().filter(r -> r.getStatus() == RiskCase.Status.OPEN).count();
        long totalResolved = all.stream().filter(r -> r.getStatus() == RiskCase.Status.RESOLVED).count();
        long highRisk = all.stream().filter(r -> r.getStatus() == RiskCase.Status.OPEN && r.getRiskLevel() >= 75).count();
        long mediumRisk = all.stream().filter(r -> r.getStatus() == RiskCase.Status.OPEN && r.getRiskLevel() >= 50 && r.getRiskLevel() < 75).count();
        long lowRisk = all.stream().filter(r -> r.getStatus() == RiskCase.Status.OPEN && r.getRiskLevel() < 50).count();
        long unassigned = all.stream().filter(r -> r.getStatus() == RiskCase.Status.OPEN && r.getAssignedAdmin() == null).count();

        return new AdminRiskCaseDto.StatsResponse(totalOpen, totalResolved, highRisk, mediumRisk, lowRisk, unassigned);
    }

    private RiskCase findOrThrow(Long id) {
        return riskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RiskCase not found: " + id));
    }
}