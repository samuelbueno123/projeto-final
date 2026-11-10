package com.cefet_rj.projeto_final.shared.observability.dtos;

import com.cefet_rj.projeto_final.shared.observability.entities.AuditLog;

import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        LocalDateTime timestamp,
        String correlationId,
        String category,
        String eventType,
        String severity,
        String principal,
        String action,
        String resourceType,
        String resourceId,
        Long condominiumId,
        String condominiumName,
        String ipAddress,
        String outcome,
        String details
) {
    public AuditLogDTO(AuditLog entity) {
        this(
                entity.getId(),
                entity.getTimestamp(),
                entity.getCorrelationId(),
                entity.getCategory(),
                entity.getEventType(),
                entity.getSeverity(),
                entity.getPrincipal(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getCondominiumId(),
                entity.getCondominiumName(),
                entity.getIpAddress(),
                entity.getOutcome(),
                entity.getDetails()
        );
    }
}
