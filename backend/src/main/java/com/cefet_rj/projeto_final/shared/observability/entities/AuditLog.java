package com.cefet_rj.projeto_final.shared.observability.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_audit_log", indexes = {
        @Index(name = "idx_audit_log_timestamp", columnList = "timestamp DESC"),
        @Index(name = "idx_audit_log_category", columnList = "category"),
        @Index(name = "idx_audit_log_severity", columnList = "severity"),
        @Index(name = "idx_audit_log_outcome", columnList = "outcome"),
        @Index(name = "idx_audit_log_principal", columnList = "principal"),
        @Index(name = "idx_audit_log_condominium", columnList = "condominium_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @Column(nullable = false, length = 50, updatable = false)
    private String category;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(nullable = false, length = 20, updatable = false)
    private String severity;

    @Column(nullable = false, length = 180, updatable = false)
    private String principal;

    @Column(nullable = false, length = 120, updatable = false)
    private String action;

    @Column(name = "resource_type", length = 80, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 100, updatable = false)
    private String resourceId;

    @Column(name = "condominium_id", updatable = false)
    private Long condominiumId;

    @Column(name = "condominium_name", length = 180, updatable = false)
    private String condominiumName;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(nullable = false, length = 30, updatable = false)
    private String outcome;

    @Column(length = 2000, updatable = false)
    private String details;
}
