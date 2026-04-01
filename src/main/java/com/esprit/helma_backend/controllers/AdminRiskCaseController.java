package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.AdminRiskCaseDto;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.UserRepository;
import com.esprit.helma_backend.services.AdminRiskCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/risk-cases")
@Tag(name = "Admin – Risk Cases", description = "Admin risk case management and oversight")
public class AdminRiskCaseController {

    private final AdminRiskCaseService service;
    private final UserRepository userRepository;

    @Value("${app.security.disabled:true}")
    private boolean securityDisabled;

    public AdminRiskCaseController(AdminRiskCaseService service,
                                   UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Admin dashboard – stats + all risk cases")
    public ResponseEntity<AdminRiskCaseDto.DashboardResponse> getDashboard(
            @RequestParam(defaultValue = "ALL") String status,
            Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(service.getDashboard(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one enriched risk case")
    public ResponseEntity<AdminRiskCaseDto.CaseResponse> getById(@PathVariable Long id,
                                                                 Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/admins")
    @Operation(summary = "List admin users for assignment")
    public ResponseEntity<List<AdminRiskCaseDto.AdminOption>> listAdmins(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(service.listAdmins());
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve a risk case")
    public ResponseEntity<AdminRiskCaseDto.CaseResponse> resolve(@PathVariable Long id,
                                                                 @RequestBody(required = false) AdminRiskCaseDto.ResolveRequest req,
                                                                 Authentication authentication) {
        Long actingAdminId = requireAdmin(authentication);
        AdminRiskCaseDto.ResolveRequest safeReq = req != null ? req : new AdminRiskCaseDto.ResolveRequest(null);
        return ResponseEntity.ok(service.resolve(id, safeReq, actingAdminId));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign an admin to a risk case")
    public ResponseEntity<AdminRiskCaseDto.CaseResponse> assign(@PathVariable Long id,
                                                                @RequestBody AdminRiskCaseDto.AssignRequest req,
                                                                Authentication authentication) {
        Long actingAdminId = requireAdmin(authentication);
        return ResponseEntity.ok(service.assign(id, req, actingAdminId));
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Reopen a resolved risk case")
    public ResponseEntity<AdminRiskCaseDto.CaseResponse> reopen(@PathVariable Long id,
                                                                Authentication authentication) {
        Long actingAdminId = requireAdmin(authentication);
        return ResponseEntity.ok(service.reopen(id, actingAdminId));
    }

    private Long requireAdmin(Authentication authentication) {
        if (securityDisabled) {
            return userRepository.findAll().stream()
                    .filter(user -> user.getRole() == User.Role.ADMIN)
                    .findFirst()
                    .map(User::getId)
                    .orElseThrow(() -> new AccessDeniedException("No ADMIN user found. Check DevDataInitializer."));
        }

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }

        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        if (admin.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Admin access required");
        }

        return admin.getId();
    }
}