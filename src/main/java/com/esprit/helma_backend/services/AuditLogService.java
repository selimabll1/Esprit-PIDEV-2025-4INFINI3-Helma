package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.AuditLogDto;
import com.esprit.helma_backend.entities.AuditLog;
import com.esprit.helma_backend.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLogDto.Response log(Long userId,
                                    String action,
                                    String entityType,
                                    Long entityId,
                                    String details) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        AuditLog saved = auditLogRepository.save(log);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto.Response> getByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogDto.Response toResponse(AuditLog log) {
        return new AuditLogDto.Response(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}