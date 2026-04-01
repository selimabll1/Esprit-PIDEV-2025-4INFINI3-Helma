package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.AuditLogDto;
import com.esprit.helma_backend.services.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AuditLogDto.Response>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.getByUser(userId));
    }
}