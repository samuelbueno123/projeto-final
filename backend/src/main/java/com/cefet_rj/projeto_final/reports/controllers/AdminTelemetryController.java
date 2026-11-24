package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.assemblies.repositories.CondominiumAssemblyRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.repositories.CondominiumSubscriptionRepository;
import com.cefet_rj.projeto_final.community.repositories.*;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.*;
import com.cefet_rj.projeto_final.financial.repositories.CondominiumExpenseRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceOrderRepository;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.observability.SystemTelemetryService;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin/telemetry")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTelemetryController {

    private final SystemTelemetryService telemetryService;
    private final RealtimeEmitterService realtimeEmitterService;
    private final AuthenticatedUserService authenticatedUserService;
    private final JdbcTemplate jdbcTemplate;

    private final UserRepository users;
    private final CondominiumRepository condominiums;
    private final MembershipRepository memberships;
    private final UnitRepository units;
    private final ResidentRepository residents;
    private final PackageRepository packages;
    private final AnnouncementRepository announcements;
    private final ReservationRepository reservations;
    private final NotificationRepository notifications;
    private final CondominiumSubscriptionRepository subscriptions;
    private final OccurrenceTicketRepository tickets;
    private final VisitorAccessRepository visitors;
    private final CondominiumAssemblyRepository assemblies;
    private final MaintenanceOrderRepository maintenanceOrders;
    private final CondominiumExpenseRepository expenses;
    private final UnitPaymentRepository unitPayments;
    private final CondominiumDocumentRepository documents;
    private final EmergencyAlertRepository emergencyAlerts;

    @GetMapping("/snapshot")
    public Map<String, Object> snapshot(@RequestParam(defaultValue = "15m") String window) {
        return telemetryService.buildSnapshot(window);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return telemetryService.registerAdminEmitter();
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", telemetryService.getActiveAlerts());
        response.put("history", telemetryService.getAlertHistory());
        response.put("summary", telemetryService.getAlertsSummary());
        return response;
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public Map<String, Object> acknowledgeAlert(@PathVariable String alertId) {
        String actor = authenticatedUserService.getAuthenticatedUser().getEmail();
        Map<String, Object> updated = telemetryService.acknowledgeAlert(alertId, actor);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta ativo não encontrado: " + alertId);
        }
        return updated;
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public Map<String, Object> resolveAlert(@PathVariable String alertId) {
        String actor = authenticatedUserService.getAuthenticatedUser().getEmail();
        Map<String, Object> resolved = telemetryService.resolveAlert(alertId, actor);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta ativo não encontrado: " + alertId);
        }
        return resolved;
    }

    @GetMapping("/business")
    @Transactional(readOnly = true)
    public Map<String, Object> business(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) Long condominiumId,
            @RequestParam(required = false) String category
    ) {
        int days = switch (range.trim().toLowerCase(Locale.ROOT)) {
            case "7d" -> 7;
            case "90d" -> 90;
            default -> 30;
        };

        long activeCondos = condominiums.countByIsActiveTrue();
        long inactiveCondos = condominiums.countByIsActiveFalse();
        long totalCondos = activeCondos + inactiveCondos;

        Map<String, Long> condosByPlan = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT COALESCE(bp.name, 'Sem Plano') AS plan_name, COUNT(c.id) AS total " +
                            "FROM tb_condominium c LEFT JOIN tb_billing_plan bp ON bp.id = c.billing_plan_id " +
                            "GROUP BY bp.name ORDER BY total DESC"
            );
            for (Map<String, Object> row : rows) {
                condosByPlan.put(String.valueOf(row.get("plan_name")), ((Number) row.get("total")).longValue());
            }
        } catch (Exception ignored) {
        }

        Map<String, Long> subscriptionsByStatus = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT status, COUNT(id) AS total FROM tb_condominium_subscription GROUP BY status"
            );
            for (Map<String, Object> row : rows) {
                subscriptionsByStatus.put(String.valueOf(row.get("status")), ((Number) row.get("total")).longValue());
            }
        } catch (Exception ignored) {
        }

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        usersByRole.put("ROLE_ADMIN", 0L);
        usersByRole.put("ROLE_MANAGER", 0L);
        usersByRole.put("ROLE_DOORMAN", 0L);
        usersByRole.put("ROLE_RESIDENT", 0L);
        for (Object[] row : users.countUsersGroupedByRole()) {
            if (row[0] != null && row[1] != null) {
                usersByRole.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            }
        }
        for (Object[] row : memberships.countActiveGroupedByAuthority()) {
            if (row[0] != null && row[1] != null) {
                usersByRole.merge(String.valueOf(row[0]), ((Number) row[1]).longValue(), Math::max);
            }
        }

        Map<String, Long> membershipsByStatus = new LinkedHashMap<>();
        for (MembershipStatus st : MembershipStatus.values()) {
            membershipsByStatus.put(st.name(), 0L);
        }
        for (Object[] row : memberships.countGroupedByStatus()) {
            if (row[0] != null && row[1] != null) {
                membershipsByStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            }
        }

        Map<String, Long> allModules = new LinkedHashMap<>();
        allModules.put("Encomendas", packages.count());
        allModules.put("Reservas", reservations.count());
        allModules.put("Visitantes", visitors.count());
        allModules.put("Ocorrências", tickets.count());
        allModules.put("Comunicados", announcements.count());
        allModules.put("Notificações", notifications.count());
        allModules.put("Manutenções", maintenanceOrders.count());
        allModules.put("Despesas", expenses.count());
        allModules.put("Boletos", unitPayments.count());
        allModules.put("Assembleias", assemblies.count());
        allModules.put("Documentos", documents.count());
        allModules.put("Alertas SOS", emergencyAlerts.count());

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            String cat = category.trim().toUpperCase(Locale.ROOT);
            allModules.entrySet().removeIf(e -> !matchesModuleCategory(e.getKey(), cat));
        }

        List<Map<String, Object>> condominiumRanking = buildCondominiumRanking(condominiumId);
        List<Map<String, Object>> activityTimeline = buildActivityTimeline(days, condominiumId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", days + "d");
        result.put("condominiumId", condominiumId);
        result.put("category", category != null ? category : "ALL");
        result.put("condominiums", Map.of(
                "total", totalCondos,
                "active", activeCondos,
                "inactive", inactiveCondos,
                "subscriptionsTotal", subscriptions.count(),
                "byPlan", condosByPlan,
                "bySubscriptionStatus", subscriptionsByStatus
        ));
        result.put("users", Map.of(
                "total", users.count(),
                "unitsTotal", units.count(),
                "residentsTotal", residents.count(),
                "activeRealtime", realtimeEmitterService.getConnectedUsersCount(),
                "byRole", usersByRole,
                "membershipsByStatus", membershipsByStatus
        ));
        result.put("moduleUsage", allModules);
        result.put("condominiumRanking", condominiumRanking);
        result.put("activityTimeline", activityTimeline);
        result.put("unavailableMetrics", List.of(
                "Novos usuários por período e novos condomínios por período não possuem coluna created_at em tb_user/tb_condominium; utiliza-se tb_condominium_user.created_at (novos vínculos) como indicador real de crescimento."
        ));
        return result;
    }

    private boolean matchesModuleCategory(String moduleName, String category) {
        return switch (category) {
            case "OPERATIONS" -> List.of("Encomendas", "Visitantes", "Ocorrências", "Alertas SOS", "Manutenções").contains(moduleName);
            case "FINANCE" -> List.of("Boletos", "Despesas").contains(moduleName);
            case "COMMUNITY" -> List.of("Reservas", "Comunicados", "Notificações", "Assembleias", "Documentos").contains(moduleName);
            default -> true;
        };
    }

    private List<Map<String, Object>> buildCondominiumRanking(Long filterCondoId) {
        try {
            String sql = """
                    SELECT c.id, c.name, c.is_active,
                           COALESCE(bp.name, 'Sem Plano') AS plan_name,
                           (SELECT COUNT(*) FROM tb_unit u JOIN tb_sector s ON s.id = u.sector_id WHERE s.condominium_id = c.id) AS units_count,
                           (SELECT COUNT(*) FROM tb_condominium_user m WHERE m.condominium_id = c.id AND m.status = 'ACTIVE') AS members_count,
                           (SELECT COUNT(*) FROM tb_package p JOIN tb_unit u ON u.id = p.unit_id JOIN tb_sector s ON s.id = u.sector_id WHERE s.condominium_id = c.id) AS packages_count,
                           (SELECT COUNT(*) FROM tb_reservation r JOIN tb_common_area ca ON ca.id = r.common_area_id WHERE ca.condominium_id = c.id) AS reservations_count,
                           (SELECT COUNT(*) FROM tb_visitor_access v WHERE v.condominium_id = c.id) AS visitors_count,
                           (SELECT COUNT(*) FROM tb_occurrence_ticket t WHERE t.condominium_id = c.id) AS tickets_count
                    FROM tb_condominium c
                    LEFT JOIN tb_billing_plan bp ON bp.id = c.billing_plan_id
                    """ + (filterCondoId != null ? "WHERE c.id = ? " : "") + "ORDER BY members_count DESC, units_count DESC LIMIT 20";

            List<Map<String, Object>> rows = filterCondoId != null
                    ? jdbcTemplate.queryForList(sql, filterCondoId)
                    : jdbcTemplate.queryForList(sql);

            List<Map<String, Object>> ranking = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                long unitsCnt = ((Number) r.get("units_count")).longValue();
                long membersCnt = ((Number) r.get("members_count")).longValue();
                long pkgCnt = ((Number) r.get("packages_count")).longValue();
                long resCnt = ((Number) r.get("reservations_count")).longValue();
                long visCnt = ((Number) r.get("visitors_count")).longValue();
                long tktCnt = ((Number) r.get("tickets_count")).longValue();
                long totalActivity = pkgCnt + resCnt + visCnt + tktCnt;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", r.get("id"));
                item.put("name", r.get("name"));
                item.put("active", r.get("is_active"));
                item.put("planName", r.get("plan_name"));
                item.put("units", unitsCnt);
                item.put("members", membersCnt);
                item.put("packages", pkgCnt);
                item.put("reservations", resCnt);
                item.put("visitors", visCnt);
                item.put("tickets", tktCnt);
                item.put("totalActivity", totalActivity);
                ranking.add(item);
            }
            return ranking;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> buildActivityTimeline(int days, Long filterCondoId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        LocalDateTime since = start.atStartOfDay();

        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            buckets.put(start.plusDays(i), new long[5]); // [packages, reservations, visitors, tickets, memberships]
        }

        try {
            String pkgSql = filterCondoId != null
                    ? "SELECT p.received_at FROM tb_package p JOIN tb_unit u ON u.id = p.unit_id JOIN tb_sector s ON s.id = u.sector_id WHERE p.received_at >= ? AND s.condominium_id = ?"
                    : "SELECT received_at FROM tb_package WHERE received_at >= ?";
            accumulateTimestamps(buckets, pkgSql, since, filterCondoId, 0);

            String resSql = filterCondoId != null
                    ? "SELECT r.created_at FROM tb_reservation r JOIN tb_common_area ca ON ca.id = r.common_area_id WHERE r.created_at >= ? AND ca.condominium_id = ?"
                    : "SELECT created_at FROM tb_reservation WHERE created_at >= ?";
            accumulateTimestamps(buckets, resSql, since, filterCondoId, 1);

            String visSql = filterCondoId != null
                    ? "SELECT created_at FROM tb_visitor_access WHERE created_at >= ? AND condominium_id = ?"
                    : "SELECT created_at FROM tb_visitor_access WHERE created_at >= ?";
            accumulateTimestamps(buckets, visSql, since, filterCondoId, 2);

            String tktSql = filterCondoId != null
                    ? "SELECT created_at FROM tb_occurrence_ticket WHERE created_at >= ? AND condominium_id = ?"
                    : "SELECT created_at FROM tb_occurrence_ticket WHERE created_at >= ?";
            accumulateTimestamps(buckets, tktSql, since, filterCondoId, 3);

            String memSql = filterCondoId != null
                    ? "SELECT created_at FROM tb_condominium_user WHERE created_at >= ? AND condominium_id = ?"
                    : "SELECT created_at FROM tb_condominium_user WHERE created_at >= ?";
            accumulateTimestamps(buckets, memSql, since, filterCondoId, 4);
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> entry : buckets.entrySet()) {
            long[] v = entry.getValue();
            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", entry.getKey().toString());
            dayMap.put("packages", v[0]);
            dayMap.put("reservations", v[1]);
            dayMap.put("visitors", v[2]);
            dayMap.put("tickets", v[3]);
            dayMap.put("memberships", v[4]);
            dayMap.put("total", v[0] + v[1] + v[2] + v[3] + v[4]);
            timeline.add(dayMap);
        }
        return timeline;
    }

    private void accumulateTimestamps(
            Map<LocalDate, long[]> buckets,
            String sql,
            LocalDateTime since,
            Long filterCondoId,
            int slotIndex
    ) {
        Timestamp tsParam = Timestamp.valueOf(since);
        List<Object> rawValues = filterCondoId != null
                ? jdbcTemplate.queryForList(sql, Object.class, tsParam, filterCondoId)
                : jdbcTemplate.queryForList(sql, Object.class, tsParam);

        for (Object raw : rawValues) {
            LocalDate date = extractLocalDate(raw);
            if (date != null) {
                long[] counts = buckets.get(date);
                if (counts != null) {
                    counts[slotIndex]++;
                }
            }
        }
    }

    private LocalDate extractLocalDate(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (raw instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (raw instanceof java.sql.Date d) return d.toLocalDate();
        try {
            return LocalDateTime.parse(raw.toString().replace(" ", "T")).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }
}
