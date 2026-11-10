package com.cefet_rj.projeto_final.shared.observability;

import com.cefet_rj.projeto_final.shared.observability.entities.AuditLog;
import com.cefet_rj.projeto_final.shared.observability.repositories.AuditLogRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOG");

    private AuditLogRepository auditLogRepository;
    private TransactionTemplate requiresNewTxTemplate;

    public AuditLogService() {
    }

    @Autowired(required = false)
    public void setAuditLogRepository(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Autowired(required = false)
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTxTemplate = template;
        }
    }

    @PostConstruct
    public void recordSystemStartup() {
        try {
            if (auditLogRepository != null && auditLogRepository.count() == 0) {
                persistAuditRecord(
                        "SYSTEM",
                        "SYSTEM_STARTUP",
                        "INFO",
                        "SYSTEM",
                        "BOOT_INITIALIZATION",
                        "APPLICATION",
                        "projeto-final-api",
                        null,
                        null,
                        "127.0.0.1",
                        "SUCCESS",
                        "Inicialização da plataforma concluída. Serviços de autenticação, banco de dados e monitoramento ativos."
                );
            }
        } catch (Exception ignored) {
        }
    }

    public void logSecurityEvent(
            String eventType,
            String principal,
            String action,
            String resourceId,
            boolean success,
            String details
    ) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        String safePrincipal = resolvePrincipal(principal);

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

        persistAuditRecord(
                "SECURITY",
                eventType != null ? eventType : "SECURITY_EVENT",
                success ? "INFO" : "WARN",
                safePrincipal,
                action != null ? action : "SECURITY_CHECK",
                "SECURITY_RESOURCE",
                resourceId,
                null,
                null,
                null,
                success ? "SUCCESS" : "FAILURE",
                details
        );
    }

    public void logAuthentication(String username, boolean success, String ipAddress, String reason) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        String safeUsername = (username != null && !username.isBlank()) ? username : "ANONYMOUS";
        auditLog.info(
                "AUDIT_AUTH: timestamp=\"{}\" correlationId=\"{}\" username=\"{}\" ipAddress=\"{}\" outcome=\"{}\" reason=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                safeUsername,
                ipAddress != null ? ipAddress : "UNKNOWN",
                success ? "SUCCESS" : "FAILURE",
                reason != null ? reason : "OK"
        );

        persistAuditRecord(
                "AUTH",
                success ? "LOGIN_SUCCESS" : "LOGIN_FAILURE",
                success ? "INFO" : "WARN",
                safeUsername,
                "AUTHENTICATE",
                "SESSION",
                safeUsername,
                null,
                null,
                ipAddress != null ? ipAddress : "UNKNOWN",
                success ? "SUCCESS" : "FAILURE",
                reason != null ? reason : (success ? "Autenticação realizada com sucesso" : "Falha de autenticação")
        );
    }

    public void logRoleAssignment(String operator, String targetUser, String role, boolean granted) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        String safeOperator = resolvePrincipal(operator);
        auditLog.info(
                "AUDIT_AUTHORIZATION: timestamp=\"{}\" correlationId=\"{}\" operator=\"{}\" targetUser=\"{}\" role=\"{}\" action=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                safeOperator,
                targetUser,
                role,
                granted ? "GRANTED" : "REVOKED"
        );

        persistAuditRecord(
                "AUTHORIZATION",
                granted ? "ROLE_GRANTED" : "ROLE_REVOKED",
                "INFO",
                safeOperator,
                granted ? "GRANT_ROLE" : "REVOKE_ROLE",
                "USER_ROLE",
                targetUser,
                null,
                null,
                null,
                "SUCCESS",
                "Perfil " + role + (granted ? " concedido para " : " removido de ") + targetUser
        );
    }

    public void logEmergencyAlert(String triggeredBy, Long alertId, String type, Long condominiumId) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        String safePrincipal = resolvePrincipal(triggeredBy);
        auditLog.warn(
                "AUDIT_EMERGENCY: timestamp=\"{}\" correlationId=\"{}\" triggeredBy=\"{}\" alertId=\"{}\" type=\"{}\" condominiumId=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                safePrincipal,
                alertId,
                type,
                condominiumId
        );

        persistAuditRecord(
                "EMERGENCY",
                "EMERGENCY_ALERT_TRIGGERED",
                "CRITICAL",
                safePrincipal,
                "TRIGGER_EMERGENCY",
                "EMERGENCY_ALERT",
                alertId != null ? String.valueOf(alertId) : null,
                condominiumId,
                null,
                null,
                "SUCCESS",
                "Alerta de emergência acionado: tipo=" + type + (condominiumId != null ? ", condomínioId=" + condominiumId : "")
        );
    }

    public void logSensitiveOperation(String principal, String operation, String target, String details) {
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        String safePrincipal = resolvePrincipal(principal);
        auditLog.info(
                "AUDIT_SENSITIVE_OP: timestamp=\"{}\" correlationId=\"{}\" principal=\"{}\" operation=\"{}\" target=\"{}\" details=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                safePrincipal,
                operation,
                target,
                sanitize(details)
        );

        persistAuditRecord(
                "ADMIN_OP",
                operation != null ? operation : "SENSITIVE_OPERATION",
                "INFO",
                safePrincipal,
                operation != null ? operation : "SENSITIVE_OPERATION",
                "RESOURCE",
                target,
                null,
                null,
                null,
                "SUCCESS",
                details
        );
    }

    public void logAdminOperation(
            String principal,
            String action,
            String resourceType,
            String resourceId,
            Long condominiumId,
            String condominiumName,
            boolean success,
            String details
    ) {
        String safePrincipal = resolvePrincipal(principal);
        String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        auditLog.info(
                "AUDIT_ADMIN_OP: timestamp=\"{}\" correlationId=\"{}\" principal=\"{}\" action=\"{}\" resourceType=\"{}\" resourceId=\"{}\" outcome=\"{}\" details=\"{}\"",
                Instant.now(),
                correlationId != null ? correlationId : "N/A",
                safePrincipal,
                action,
                resourceType,
                resourceId,
                success ? "SUCCESS" : "FAILURE",
                sanitize(details)
        );

        persistAuditRecord(
                "ADMIN_OP",
                action != null ? action : "ADMIN_ACTION",
                success ? "INFO" : "WARN",
                safePrincipal,
                action != null ? action : "ADMIN_ACTION",
                resourceType,
                resourceId,
                condominiumId,
                condominiumName,
                null,
                success ? "SUCCESS" : "FAILURE",
                details
        );
    }

    public void logErrorEvent(
            String principal,
            String eventType,
            String action,
            String path,
            String ipAddress,
            String severity,
            String details
    ) {
        String safePrincipal = resolvePrincipal(principal);
        persistAuditRecord(
                "ERROR",
                eventType != null ? eventType : "SYSTEM_ERROR",
                severity != null ? severity : "ERROR",
                safePrincipal,
                action != null ? action : "HTTP_REQUEST",
                "ENDPOINT",
                path,
                null,
                null,
                ipAddress,
                "FAILURE",
                details
        );
    }

    public void persistAuditRecord(
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
        if (auditLogRepository == null) {
            return;
        }
        try {
            String correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            AuditLog record = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .correlationId(truncate(correlationId, 100))
                    .category(truncate(category != null ? category : "SYSTEM", 50))
                    .eventType(truncate(eventType != null ? eventType : "EVENT", 100))
                    .severity(truncate(severity != null ? severity : "INFO", 20))
                    .principal(truncate(resolvePrincipal(principal), 180))
                    .action(truncate(action != null ? action : "OPERATION", 120))
                    .resourceType(truncate(resourceType, 80))
                    .resourceId(truncate(resourceId, 100))
                    .condominiumId(condominiumId)
                    .condominiumName(truncate(condominiumName, 180))
                    .ipAddress(truncate(ipAddress, 64))
                    .outcome(truncate(outcome != null ? outcome : "SUCCESS", 30))
                    .details(truncate(sanitize(details), 2000))
                    .build();

            if (requiresNewTxTemplate != null) {
                requiresNewTxTemplate.executeWithoutResult(status -> auditLogRepository.save(record));
            } else {
                auditLogRepository.save(record);
            }
        } catch (Exception ignored) {
            // Never allow audit persistence failure to break application flow
        }
    }

    public String resolveCurrentPrincipal() {
        return resolvePrincipal(null);
    }

    private String resolvePrincipal(String providedPrincipal) {
        if (providedPrincipal != null && !providedPrincipal.isBlank()) {
            return providedPrincipal.trim();
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "ANONYMOUS";
    }

    private String sanitize(String input) {
        if (input == null) return "";
        return input.replace("\r", " ").replace("\n", " ").trim();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
