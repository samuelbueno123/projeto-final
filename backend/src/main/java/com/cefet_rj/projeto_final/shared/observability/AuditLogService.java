package com.cefet_rj.projeto_final.shared.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOG");

    public void logSecurityEvent(
            String eventType,
            String principal,
            String action,
            String resourceId,
            boolean success,
            String details
    ) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        String safePrincipal = (principal != null && !principal.isBlank()) ? principal : "ANONYMOUS";

        auditLog.info(
                "AUDIT_EVENT: timestamp=\"{}\" correlationId=\"{}\" eventType=\"{}\" principal=\"{}\" action=\"{}\" resourceId=\"{}\" outcome=\"{}\" details=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                eventType,
                safePrincipal,
                action,
                resourceId != null ? resourceId : "N/A",
                success ? "SUCCESS" : "FAILURE",
                sanitize(details)
        );
    }

    public void logAuthentication(String username, boolean success, String ipAddress, String reason) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        auditLog.info(
                "AUDIT_AUTH: timestamp=\"{}\" correlationId=\"{}\" username=\"{}\" ipAddress=\"{}\" outcome=\"{}\" reason=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                username,
                ipAddress != null ? ipAddress : "UNKNOWN",
                success ? "SUCCESS" : "FAILURE",
                reason != null ? reason : "OK"
        );
    }

    public void logRoleAssignment(String operator, String targetUser, String role, boolean granted) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        auditLog.info(
                "AUDIT_AUTHORIZATION: timestamp=\"{}\" correlationId=\"{}\" operator=\"{}\" targetUser=\"{}\" role=\"{}\" action=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                operator,
                targetUser,
                role,
                granted ? "GRANTED" : "REVOKED"
        );
    }

    public void logEmergencyAlert(String triggeredBy, Long alertId, String type, Long condominiumId) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        auditLog.warn(
                "AUDIT_EMERGENCY: timestamp=\"{}\" correlationId=\"{}\" triggeredBy=\"{}\" alertId=\"{}\" type=\"{}\" condominiumId=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                triggeredBy,
                alertId,
                type,
                condominiumId
        );
    }

    public void logSensitiveOperation(String principal, String operation, String target, String details) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        auditLog.info(
                "AUDIT_SENSITIVE_OP: timestamp=\"{}\" correlationId=\"{}\" principal=\"{}\" operation=\"{}\" target=\"{}\" details=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                principal,
                operation,
                target,
                sanitize(details)
        );
    }

    private String sanitize(String input) {
        if (input == null) return "";
        // Prevent CRLF injection in audit logs
        return input.replace("\r", " ").replace("\n", " ").trim();
    }
}
