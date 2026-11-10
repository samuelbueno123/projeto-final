package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.repositories.CondominiumAssemblyRepository;
import com.cefet_rj.projeto_final.billing.dtos.BillingPlanDTO;
import com.cefet_rj.projeto_final.billing.dtos.FeatureDTO;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.community.repositories.*;
import com.cefet_rj.projeto_final.community.services.CondominiumCommunityService.*;
import com.cefet_rj.projeto_final.condominiums.dtos.*;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.*;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO;
import com.cefet_rj.projeto_final.financial.repositories.CondominiumExpenseRepository;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceOrderRepository;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.packages.dtos.PackageDTO;
import com.cefet_rj.projeto_final.packages.history.dtos.PackageHistoryDTO;
import com.cefet_rj.projeto_final.packages.history.repositories.PackageHistoryRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.reports.dtos.AdminDashboardDTO;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaDTO;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationDTO;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.observability.AuditLogService;
import com.cefet_rj.projeto_final.shared.observability.OperationalMetricsService;
import com.cefet_rj.projeto_final.shared.observability.dtos.AuditLogDTO;
import com.cefet_rj.projeto_final.shared.observability.entities.AuditLog;
import com.cefet_rj.projeto_final.shared.observability.repositories.AuditLogRepository;
import com.cefet_rj.projeto_final.users.dtos.UserDTO;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminDashboardController {

    private final UserRepository users;
    private final CondominiumRepository condominiums;
    private final MembershipRepository memberships;
    private final SectorRepository sectors;
    private final UnitRepository units;
    private final ResidentRepository residents;
    private final PackageRepository packages;
    private final PackageHistoryRepository packageHistories;
    private final AnnouncementRepository announcements;
    private final ReservationRepository reservations;
    private final CommonAreaRepository commonAreas;
    private final NotificationRepository notifications;
    private final BillingPlanRepository billingPlans;
    private final FeatureRepository features;
    private final OccurrenceTicketRepository tickets;
    private final VisitorAccessRepository visitors;
    private final LostFoundItemRepository lostFound;
    private final ClassifiedAdRepository classifieds;
    private final VehicleRepository vehicles;
    private final PetRepository pets;
    private final MovingRequestRepository movingRequests;
    private final CondominiumDocumentRepository documents;
    private final SmartLockerCompartmentRepository lockers;
    private final EventGuestListRepository guestLists;
    private final CondominiumInfractionRepository infractions;
    private final CondominiumAssemblyRepository assemblies;
    private final MaintenanceOrderRepository maintenanceOrders;
    private final CondominiumExpenseRepository expenses;
    private final UnitPaymentRepository unitPayments;
    private final EmergencyAlertRepository emergencyAlerts;
    private final ConciergeShiftLogRepository shiftLogs;
    private final RenovationRequestRepository renovationRequests;
    private final KeyRegistryEntryRepository keyRegistryEntries;
    private final UtilityReadingRepository utilityReadings;
    private final ParkingSpotRepository parkingSpots;
    private final PollRepository polls;
    private final AuditLogRepository auditLogs;
    private final AuditLogService auditLogService;
    private final OperationalMetricsService operationalMetricsService;
    private final RealtimeEmitterService realtimeEmitterService;
    private final CacheManager cacheManager;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard")
    public AdminDashboardDTO dashboard() {
        return new AdminDashboardDTO(
                users.count(),
                condominiums.count(),
                memberships.count(),
                sectors.count(),
                units.count(),
                residents.count(),
                packages.count(),
                announcements.count(),
                reservations.count(),
                commonAreas.count(),
                notifications.count(),
                billingPlans.count(),
                features.count()
        );
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<Condominium> allCondos = condominiums.findAll();
        long activeCondos = allCondos.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).count();
        long inactiveCondos = allCondos.size() - activeCondos;

        List<User> allUsers = users.findAll();
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        usersByRole.put("ROLE_ADMIN", 0L);
        usersByRole.put("ROLE_MANAGER", 0L);
        usersByRole.put("ROLE_DOORMAN", 0L);
        usersByRole.put("ROLE_RESIDENT", 0L);

        for (User u : allUsers) {
            if (u.getRoles() != null) {
                for (Role r : u.getRoles()) {
                    if (r.getAuthority() != null) {
                        usersByRole.merge(r.getAuthority(), 1L, Long::sum);
                    }
                }
            }
        }

        List<Membership> allMemberships = memberships.findAll();
        Map<String, Long> membershipsByStatus = new LinkedHashMap<>();
        for (MembershipStatus st : MembershipStatus.values()) {
            membershipsByStatus.put(st.name(), 0L);
        }
        for (Membership m : allMemberships) {
            if (m.getStatus() != null) {
                membershipsByStatus.merge(m.getStatus().name(), 1L, Long::sum);
            }
            if (m.getRole() != null && m.getRole().getAuthority() != null && m.getStatus() == MembershipStatus.ACTIVE) {
                // Also ensure users whose role comes via condominium membership are represented
                usersByRole.putIfAbsent(m.getRole().getAuthority(), 0L);
            }
        }

        Map<String, Long> moduleVolumes = new LinkedHashMap<>();
        moduleVolumes.put("users", (long) allUsers.size());
        moduleVolumes.put("condominiums", (long) allCondos.size());
        moduleVolumes.put("memberships", (long) allMemberships.size());
        moduleVolumes.put("sectors", sectors.count());
        moduleVolumes.put("units", units.count());
        moduleVolumes.put("residents", residents.count());
        moduleVolumes.put("packages", packages.count());
        moduleVolumes.put("packageHistory", packageHistories.count());
        moduleVolumes.put("announcements", announcements.count());
        moduleVolumes.put("reservations", reservations.count());
        moduleVolumes.put("commonAreas", commonAreas.count());
        moduleVolumes.put("notifications", notifications.count());
        moduleVolumes.put("billingPlans", billingPlans.count());
        moduleVolumes.put("features", features.count());
        moduleVolumes.put("tickets", tickets.count());
        moduleVolumes.put("visitors", visitors.count());
        moduleVolumes.put("lostFound", lostFound.count());
        moduleVolumes.put("classifieds", classifieds.count());
        moduleVolumes.put("vehicles", vehicles.count());
        moduleVolumes.put("pets", pets.count());
        moduleVolumes.put("movingRequests", movingRequests.count());
        moduleVolumes.put("documents", documents.count());
        moduleVolumes.put("lockers", lockers.count());
        moduleVolumes.put("guestLists", guestLists.count());
        moduleVolumes.put("infractions", infractions.count());
        moduleVolumes.put("assemblies", assemblies.count());
        moduleVolumes.put("maintenanceOrders", maintenanceOrders.count());
        moduleVolumes.put("expenses", expenses.count());
        moduleVolumes.put("unitPayments", unitPayments.count());
        moduleVolumes.put("emergencyAlerts", emergencyAlerts.count());
        moduleVolumes.put("shiftLogs", shiftLogs.count());
        moduleVolumes.put("renovationRequests", renovationRequests.count());
        moduleVolumes.put("keyRegistry", keyRegistryEntries.count());
        moduleVolumes.put("utilityReadings", utilityReadings.count());
        moduleVolumes.put("parkingSpots", parkingSpots.count());
        moduleVolumes.put("polls", polls.count());
        moduleVolumes.put("auditLogs", auditLogs.count());

        Map<String, Long> auditByCategory = toCountMap(auditLogs.countGroupedByCategory());
        Map<String, Long> auditBySeverity = toCountMap(auditLogs.countGroupedBySeverity());
        Map<String, Long> auditByOutcome = toCountMap(auditLogs.countGroupedByOutcome());

        List<AuditLogDTO> recentAuditLogs = auditLogs.findTop15ByOrderByTimestampDesc()
                .stream()
                .map(AuditLogDTO::new)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeCondominiums", activeCondos);
        response.put("inactiveCondominiums", inactiveCondos);
        response.put("usersByRole", usersByRole);
        response.put("membershipsByStatus", membershipsByStatus);
        response.put("moduleVolumes", moduleVolumes);
        response.put("auditByCategory", auditByCategory);
        response.put("auditBySeverity", auditBySeverity);
        response.put("auditByOutcome", auditByOutcome);
        response.put("recentAuditLogs", recentAuditLogs);
        response.put("activeSseConnections", realtimeEmitterService.getActiveConnectionsCount());
        response.put("connectedRealtimeUsers", realtimeEmitterService.getConnectedUsersCount());
        return response;
    }

    @GetMapping("/audit-logs")
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> listAuditLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String principal,
            @RequestParam(required = false) Long condominiumId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @Size(max = 120) String query,
            Pageable pageable
    ) {
        LocalDateTime startDateTime = parseDateTimeParam(startDate, false);
        LocalDateTime endDateTime = parseDateTimeParam(endDate, true);

        Specification<AuditLog> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDateTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDateTime));
            }
            if (endDateTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDateTime));
            }
            if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase(Locale.ROOT)));
            }
            if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
                predicates.add(cb.equal(cb.upper(root.get("severity")), severity.trim().toUpperCase(Locale.ROOT)));
            }
            if (outcome != null && !outcome.isBlank() && !"ALL".equalsIgnoreCase(outcome)) {
                predicates.add(cb.equal(cb.upper(root.get("outcome")), outcome.trim().toUpperCase(Locale.ROOT)));
            }
            if (principal != null && !principal.isBlank()) {
                String pattern = "%" + principal.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("principal")), pattern));
            }
            if (condominiumId != null) {
                predicates.add(cb.equal(root.get("condominiumId"), condominiumId));
            }
            if (action != null && !action.isBlank()) {
                String pattern = "%" + action.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("eventType")), pattern)
                ));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("principal")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("eventType")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("resourceType"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("resourceId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("details"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("correlationId"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("ipAddress"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable sortedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(
                        pageable.isUnpaged() ? 0 : pageable.getPageNumber(),
                        pageable.isUnpaged() ? 50 : pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "timestamp", "id")
                );

        return auditLogs.findAll(spec, sortedPageable).map(AuditLogDTO::new);
    }

    @GetMapping("/audit-logs/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> auditLogStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", auditLogs.count());
        stats.put("byCategory", toCountMap(auditLogs.countGroupedByCategory()));
        stats.put("bySeverity", toCountMap(auditLogs.countGroupedBySeverity()));
        stats.put("byOutcome", toCountMap(auditLogs.countGroupedByOutcome()));
        return stats;
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics() {
        long dbStartNs = System.nanoTime();
        boolean dbOk = false;
        try {
            Integer res = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbOk = (res != null && res == 1);
        } catch (Exception ignored) {
        }
        long dbLatencyMs = Math.max(0L, (System.nanoTime() - dbStartNs) / 1_000_000L);

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsedMb = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMaxMb = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long nonHeapUsedMb = memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
        double heapUsagePercent = heapMaxMb > 0 ? Math.round(((double) heapUsedMb / heapMaxMb) * 1000.0) / 10.0 : 0.0;

        File root = new File(".");
        long freeDiskMb = root.getFreeSpace() / (1024 * 1024);
        long totalDiskMb = root.getTotalSpace() / (1024 * 1024);
        double diskUsagePercent = totalDiskMb > 0
                ? Math.round(((double) (totalDiskMb - freeDiskMb) / totalDiskMb) * 1000.0) / 10.0
                : 0.0;

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = runtimeBean.getUptime() / 1000L;

        List<Map<String, Object>> cachesInfo = new ArrayList<>();
        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                int entryCount = 0;
                if (cache != null && cache.getNativeCache() instanceof ConcurrentMap<?, ?> map) {
                    entryCount = map.size();
                }
                cachesInfo.add(Map.of(
                        "name", cacheName,
                        "entries", entryCount,
                        "provider", cache != null ? cache.getClass().getSimpleName() : "SimpleCache"
                ));
            }
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("loginSuccess", operationalMetricsService.getLoginSuccessCount());
        metrics.put("loginFailure", operationalMetricsService.getLoginFailureCount());
        metrics.put("reservationCreated", operationalMetricsService.getReservationCreatedCount());
        metrics.put("reservationConflict", operationalMetricsService.getReservationConflictCount());
        metrics.put("packageReceived", operationalMetricsService.getPackageReceivedCount());
        metrics.put("packageDelivered", operationalMetricsService.getPackageDeliveredCount());
        metrics.put("emergencyTriggered", operationalMetricsService.getEmergencyTriggeredCount());
        metrics.put("emergencyResolved", operationalMetricsService.getEmergencyResolvedCount());
        metrics.put("visitorCheckIn", operationalMetricsService.getVisitorCheckInCount());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("overallStatus", (dbOk && heapUsagePercent < 98.0) ? "UP" : "DEGRADED");
        result.put("database", Map.of(
                "status", dbOk ? "UP" : "DOWN",
                "latencyMs", dbLatencyMs,
                "validationQuery", "SELECT 1"
        ));
        result.put("jvm", Map.of(
                "status", heapUsagePercent < 95.0 ? "UP" : "WARN",
                "heapUsedMb", heapUsedMb,
                "heapMaxMb", heapMaxMb,
                "nonHeapUsedMb", nonHeapUsedMb,
                "heapUsagePercent", heapUsagePercent,
                "javaVersion", System.getProperty("java.version", "17"),
                "javaVendor", System.getProperty("java.vendor", "OpenJDK"),
                "availableProcessors", Runtime.getRuntime().availableProcessors(),
                "uptimeSeconds", uptimeSeconds
        ));
        result.put("disk", Map.of(
                "status", freeDiskMb >= 50 ? "UP" : "WARN",
                "freeMb", freeDiskMb,
                "totalMb", totalDiskMb,
                "usagePercent", diskUsagePercent
        ));
        result.put("realtime", Map.of(
                "status", "UP",
                "activeConnections", realtimeEmitterService.getActiveConnectionsCount(),
                "connectedUsers", realtimeEmitterService.getConnectedUsersCount(),
                "connectedCondominiums", realtimeEmitterService.getConnectedCondominiumsCount()
        ));
        result.put("caches", cachesInfo);
        result.put("operationalMetrics", metrics);
        return result;
    }

    @PostMapping("/diagnostics/cache/evict")
    public Map<String, Object> evictCaches(@RequestParam(required = false) String cacheName) {
        List<String> cleared = new ArrayList<>();
        if (cacheManager != null) {
            if (cacheName != null && !cacheName.isBlank()) {
                Cache c = cacheManager.getCache(cacheName.trim());
                if (c != null) {
                    c.clear();
                    cleared.add(cacheName.trim());
                }
            } else {
                for (String name : cacheManager.getCacheNames()) {
                    Cache c = cacheManager.getCache(name);
                    if (c != null) {
                        c.clear();
                        cleared.add(name);
                    }
                }
            }
        }

        auditLogService.logAdminOperation(
                null,
                "CLEAR_SYSTEM_CACHE",
                "SPRING_CACHE",
                cleared.isEmpty() ? "NONE" : String.join(",", cleared),
                null,
                null,
                true,
                "Limpeza manual de cache executada pela Central Administrativa: " + cleared
        );

        return Map.of(
                "status", "CLEARED",
                "clearedCaches", cleared,
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/permissions-matrix")
    @Transactional(readOnly = true)
    public Map<String, Object> permissionsMatrix() {
        List<Map<String, Object>> rolesMatrix = List.of(
                Map.of(
                        "role", "ROLE_ADMIN",
                        "label", "Administrador Global (Super Admin)",
                        "scope", "Plataforma Global",
                        "permissions", List.of(
                                "Acesso irrestrito à Central de Gerenciamento, Logs de Auditoria e Diagnóstico",
                                "Criação, edição, ativação e desativação de condomínios e planos comerciais",
                                "Gestão global de usuários, atribuição de síndicos (ROLE_MANAGER) e vínculos",
                                "Disparo de comunicados globais de sistema e limpeza de caches",
                                "Supervisão completa de todas as 30+ entidades operacionais e financeiras"
                        )
                ),
                Map.of(
                        "role", "ROLE_MANAGER",
                        "label", "Síndico / Gestor Condominial",
                        "scope", "Escopo do Condomínio Vinculado",
                        "permissions", List.of(
                                "Gestão de blocos/setores, unidades, moradores e aprovação de solicitações de vínculo",
                                "Abertura e condução de assembleias virtuais, pautas e apuração de votos",
                                "Gestão financeira (boletos, despesas, previsão orçamentária e relatórios CSV/PDF)",
                                "Aprovação de reservas, pedidos de mudança, solicitações de obras e resposta a chamados",
                                "Registro de infrações/advertências/multas e gestão de manutenções preventivas/corretivas"
                        )
                ),
                Map.of(
                        "role", "ROLE_DOORMAN",
                        "label", "Porteiro / Agente de Portaria",
                        "scope", "Operação de Portaria do Condomínio",
                        "permissions", List.of(
                                "Recebimento, notificação e baixa de entrega de encomendas e armários inteligentes",
                                "Controle de acesso de visitantes (QR Code / PIN), leitura de placas e vagas",
                                "Registro de livro de passagem de plantão e controle de empréstimo de chaves",
                                "Atendimento prioritário a alertas de emergência e chat da portaria",
                                "Leitura de hidrômetros/gás e gerenciamento de achados e perdidos"
                        )
                ),
                Map.of(
                        "role", "ROLE_RESIDENT",
                        "label", "Morador / Condômino",
                        "scope", "Unidade Habitacional e Áreas Comuns",
                        "permissions", List.of(
                                "Acompanhamento de encomendas, solicitação de retirada e histórico de status",
                                "Solicitação de reservas de áreas comuns, mudanças e autorização de obras",
                                "Pré-cadastro de visitantes com convite QR Code e criação de listas de convidados",
                                "Participação em assembleias, enquetes, mural de classificados e abertura de chamados",
                                "Consulta de boletos da unidade, documentos do condomínio e acionamento de botão de pânico"
                        )
                )
        );

        List<BillingPlanDTO> plans = billingPlans.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(BillingPlanDTO::new)
                .toList();

        List<FeatureDTO> allFeatures = features.findAll(Sort.by(Sort.Direction.ASC, "code"))
                .stream()
                .map(FeatureDTO::new)
                .toList();

        return Map.of(
                "roles", rolesMatrix,
                "billingPlans", plans,
                "features", allFeatures
        );
    }

    @GetMapping("/data/{resource}")
    @Transactional(readOnly = true)
    public Page<?> data(
            @PathVariable String resource,
            @RequestParam(required = false) @Size(max = 100) String query,
            Pageable pageable
    ) {
        return switch (resource) {
            case "users" -> queryPage(users, UserDTO::new, query, pageable);
            case "condominiums" -> queryPage(condominiums, CondominiumDTO::new, query, pageable);
            case "memberships" -> queryPage(memberships, MembershipDTO::new, query, pageable);
            case "sectors" -> queryPage(sectors, SectorDTO::new, query, pageable);
            case "units" -> queryPage(units, UnitDTO::new, query, pageable);
            case "residents" -> queryPage(residents, ResidentDTO::new, query, pageable);
            case "packages" -> queryPage(packages, PackageDTO::new, query, pageable);
            case "package-history" -> queryPage(packageHistories, PackageHistoryDTO::new, query, pageable);
            case "announcements" -> queryPage(announcements, AnnouncementDTO::new, query, pageable);
            case "reservations" -> queryPage(reservations, ReservationDTO::new, query, pageable);
            case "common-areas" -> queryPage(commonAreas, CommonAreaDTO::new, query, pageable);
            case "notifications" -> queryPage(notifications, NotificationDTO::new, query, pageable);
            case "billing-plans" -> queryPage(billingPlans, BillingPlanDTO::new, query, pageable);
            case "features" -> queryPage(features, FeatureDTO::new, query, pageable);
            case "tickets" -> queryPage(tickets, OccurrenceTicketDTO::new, query, pageable);
            case "visitors" -> queryPage(visitors, VisitorAccessDTO::new, query, pageable);
            case "lost-found" -> queryPage(lostFound, LostFoundItemDTO::new, query, pageable);
            case "classifieds" -> queryPage(classifieds, ClassifiedAdDTO::new, query, pageable);
            case "vehicles" -> queryPage(vehicles, VehicleDTO::new, query, pageable);
            case "pets" -> queryPage(pets, PetDTO::new, query, pageable);
            case "moving-requests" -> queryPage(movingRequests, MovingRequestDTO::new, query, pageable);
            case "documents" -> queryPage(documents, CondominiumDocumentDTO::new, query, pageable);
            case "lockers" -> queryPage(lockers, SmartLockerDTO::new, query, pageable);
            case "guest-lists" -> queryPage(guestLists, EventGuestListDTO::new, query, pageable);
            case "infractions" -> queryPage(infractions, CondominiumInfractionDTO::new, query, pageable);
            case "assemblies" -> queryPage(assemblies, AssemblyDTO::new, query, pageable);
            case "maintenance-orders" -> queryPage(maintenanceOrders, MaintenanceOrderDTO::new, query, pageable);
            case "expenses" -> queryPage(expenses, ExpenseDTO::new, query, pageable);
            case "emergency-alerts" -> queryPage(emergencyAlerts, EmergencyAlertDTO::new, query, pageable);
            case "shift-logs" -> queryPage(shiftLogs, ConciergeShiftLogDTO::new, query, pageable);
            case "renovation-requests" -> queryPage(renovationRequests, RenovationRequestDTO::new, query, pageable);
            case "key-registry" -> queryPage(keyRegistryEntries, KeyRegistryEntryDTO::new, query, pageable);
            case "utility-readings" -> queryPage(utilityReadings, UtilityReadingDTO::new, query, pageable);
            case "parking-spots" -> queryPage(parkingSpots, ParkingSpotDTO::new, query, pageable);
            case "polls" -> queryPage(polls, p -> new PollDTO(
                    p.getId(),
                    p.getCondominium().getId(),
                    p.getTitle(),
                    p.getDescription(),
                    p.getActive(),
                    p.getEndsAt(),
                    p.getCreatedAt(),
                    0,
                    null,
                    List.of(),
                    p.getCreatedBy() != null ? (p.getCreatedBy().getFirstName() + " " + p.getCreatedBy().getLastName()).trim() : "Sistema"
            ), query, pageable);
            case "audit-logs" -> queryPage(auditLogs, AuditLogDTO::new, query, pageable);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown admin resource: " + resource);
        };
    }

    private <E, D> Page<D> queryPage(
            JpaRepository<E, ?> repository,
            Function<E, D> mapper,
            String query,
            Pageable pageable
    ) {
        if (query == null || query.isBlank()) {
            return repository.findAll(pageable).map(mapper);
        }

        List<String> terms = Arrays.stream(query.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();

        if (terms.isEmpty()) {
            return repository.findAll(pageable).map(mapper);
        }

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");

        List<D> filtered = repository.findAll(sort).stream()
                .map(mapper)
                .filter(dto -> matchesTerms(dto, terms))
                .toList();

        if (pageable.isUnpaged()) {
            return new PageImpl<>(filtered);
        }

        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    private boolean matchesTerms(Object dto, List<String> terms) {
        String searchable = extractSearchableText(dto).toLowerCase(Locale.ROOT);
        return terms.stream().allMatch(searchable::contains);
    }

    private String extractSearchableText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Record record) {
            StringBuilder sb = new StringBuilder();
            for (RecordComponent component : record.getClass().getRecordComponents()) {
                try {
                    Object fieldValue = component.getAccessor().invoke(record);
                    sb.append(extractSearchableText(fieldValue)).append(' ');
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return sb.toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder sb = new StringBuilder();
            for (Object item : collection) {
                sb.append(extractSearchableText(item)).append(' ');
            }
            return sb.toString();
        }
        return value.toString();
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] instanceof Number num) {
                    result.put(String.valueOf(row[0]), num.longValue());
                }
            }
        }
        return result;
    }

    private LocalDateTime parseDateTimeParam(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (Exception ignored) {
        }
        try {
            LocalDate date = LocalDate.parse(trimmed);
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (Exception ignored) {
        }
        return null;
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public AdminSearchResponse search(@RequestParam @NotBlank @Size(max = 100) String query) {
        String term = query.trim();
        if (term.isEmpty()) return new AdminSearchResponse(List.of(), List.of(), List.of());
        Pageable limit = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
        List<AdminSearchResult> userResults = users.searchByName(term, limit).stream()
                .map(user -> new AdminSearchResult(user.getId(), user.getFirstName() + " " + user.getLastName(), user.getEmail()))
                .toList();
        List<AdminSearchResult> condominiumResults = condominiums.findByNameContainingIgnoreCase(term, limit).stream()
                .map(condominium -> new AdminSearchResult(condominium.getId(), condominium.getName(), condominium.getAddress()))
                .toList();
        List<AdminSearchResult> sectorResults = sectors.findByNameContainingIgnoreCase(term, limit).stream()
                .map(sector -> new AdminSearchResult(sector.getId(), sector.getName(), sector.getCondominium().getName()))
                .toList();
        return new AdminSearchResponse(userResults, condominiumResults, sectorResults);
    }

    public record AdminSearchResult(Long id, String name, String details) {}
    public record AdminSearchResponse(List<AdminSearchResult> users,
                                      List<AdminSearchResult> condominiums,
                                      List<AdminSearchResult> sectors) {}
}
