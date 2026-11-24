package com.cefet_rj.projeto_final.shared.observability;

import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Serviço central de telemetria real da plataforma:
 * coleta infraestrutura (CPU, RAM, JVM, Disco, Threads), HTTP, Banco (Hikari/Postgres/H2),
 * séries temporais, motor de alertas e streaming SSE administrativo.
 */
@Slf4j
@Service
public class SystemTelemetryService {

    private static final int MAX_HISTORY_POINTS = 360;
    private static final int MAX_ALERT_HISTORY = 200;
    private static final int MAX_ADMIN_EMITTERS = 5;
    private static final long ADMIN_SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final HttpTelemetryFilter httpTelemetryFilter;
    private final OperationalMetricsService operationalMetricsService;
    private final RealtimeEmitterService realtimeEmitterService;
    private final AuditLogService auditLogService;
    private final CacheManager cacheManager;

    private final Deque<TelemetryPoint> history = new ArrayDeque<>(MAX_HISTORY_POINTS);
    private final Object historyLock = new Object();

    private final ConcurrentMap<String, AlertRecord> activeAlertsByRule = new ConcurrentHashMap<>();
    private final Deque<AlertRecord> alertHistory = new ArrayDeque<>(MAX_ALERT_HISTORY);
    private final Object alertLock = new Object();

    private final CopyOnWriteArrayList<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();

    private final LongAdder dbConnectionErrors = new LongAdder();
    private final AtomicLong lastTxCount = new AtomicLong(-1L);
    private final AtomicLong lastTxSampleMs = new AtomicLong(0L);
    private final AtomicLong lastQps = new AtomicLong(0L);
    private final AtomicLong lastAlertLoginFailures = new AtomicLong(0L);

    public SystemTelemetryService(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            HttpTelemetryFilter httpTelemetryFilter,
            OperationalMetricsService operationalMetricsService,
            RealtimeEmitterService realtimeEmitterService,
            AuditLogService auditLogService,
            @Autowired(required = false) CacheManager cacheManager
    ) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.httpTelemetryFilter = httpTelemetryFilter;
        this.operationalMetricsService = operationalMetricsService;
        this.realtimeEmitterService = realtimeEmitterService;
        this.auditLogService = auditLogService;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void initSample() {
        try {
            collectSample();
        } catch (Exception e) {
            log.debug("Amostra inicial de telemetria adiada: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 10000, scheduler = "sseHeartbeatScheduler")
    public void collectScheduledSample() {
        try {
            collectSample();
        } catch (Exception e) {
            log.debug("Falha ao coletar amostra de telemetria: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5000, scheduler = "sseHeartbeatScheduler")
    public void pushAdminSseUpdates() {
        if (adminEmitters.isEmpty()) {
            return;
        }
        Map<String, Object> payload = buildSnapshot("15m", true);
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : adminEmitters) {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event()
                            .name("TELEMETRY_UPDATE")
                            .data(payload));
                }
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) {
            adminEmitters.removeAll(dead);
        }
    }

    public SseEmitter registerAdminEmitter() {
        while (adminEmitters.size() >= MAX_ADMIN_EMITTERS) {
            SseEmitter oldest = adminEmitters.remove(0);
            try {
                oldest.complete();
            } catch (Exception ignored) {
            }
        }
        SseEmitter emitter = new SseEmitter(ADMIN_SSE_TIMEOUT_MS);
        adminEmitters.add(emitter);

        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            adminEmitters.remove(emitter);
        });
        emitter.onError(e -> adminEmitters.remove(emitter));

        try {
            Map<String, Object> initial = buildSnapshot("15m", true);
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name("TELEMETRY_UPDATE")
                        .data(initial));
            }
        } catch (Exception e) {
            adminEmitters.remove(emitter);
        }
        return emitter;
    }

    public Map<String, Object> buildSnapshot(String window) {
        return buildSnapshot(window, false);
    }

    private Map<String, Object> buildSnapshot(String window, boolean compact) {
        Instant now = Instant.now();
        HttpTelemetryFilter.HttpSnapshot httpSnap = httpTelemetryFilter.getSnapshot();
        InfrastructureReadout infra = readInfrastructure();
        DatabaseSummary dbSummary = readDatabaseSummary(compact);

        List<TelemetryPoint> windowPoints = getPointsForWindow(window);
        if (windowPoints.isEmpty()) {
            TelemetryPoint current = toTelemetryPoint(now, infra, httpSnap, dbSummary);
            windowPoints = List.of(current);
        }

        double avgSysCpu = round1(windowPoints.stream().mapToDouble(TelemetryPoint::systemCpuPercent).average().orElse(infra.systemCpuPercent()));
        double peakSysCpu = round1(windowPoints.stream().mapToDouble(TelemetryPoint::systemCpuPercent).max().orElse(infra.systemCpuPercent()));
        double avgProcCpu = round1(windowPoints.stream().mapToDouble(TelemetryPoint::processCpuPercent).average().orElse(infra.processCpuPercent()));
        double peakProcCpu = round1(windowPoints.stream().mapToDouble(TelemetryPoint::processCpuPercent).max().orElse(infra.processCpuPercent()));

        Map<String, Object> cpuMap = new LinkedHashMap<>();
        cpuMap.put("systemPercent", infra.systemCpuPercent());
        cpuMap.put("processPercent", infra.processCpuPercent());
        cpuMap.put("systemAvgPercent", avgSysCpu);
        cpuMap.put("systemPeakPercent", peakSysCpu);
        cpuMap.put("processAvgPercent", avgProcCpu);
        cpuMap.put("processPeakPercent", peakProcCpu);
        cpuMap.put("availableProcessors", infra.availableProcessors());
        cpuMap.put("systemLoadAverage", infra.systemLoadAverage());

        Map<String, Object> ramMap = new LinkedHashMap<>();
        ramMap.put("totalMb", infra.ramTotalMb());
        ramMap.put("usedMb", infra.ramUsedMb());
        ramMap.put("freeMb", infra.ramFreeMb());
        ramMap.put("usagePercent", infra.ramUsagePercent());
        ramMap.put("available", infra.ramTotalMb() > 0);

        Map<String, Object> jvmMap = new LinkedHashMap<>();
        jvmMap.put("heapUsedMb", infra.heapUsedMb());
        jvmMap.put("heapMaxMb", infra.heapMaxMb());
        jvmMap.put("heapCommittedMb", infra.heapCommittedMb());
        jvmMap.put("heapUsagePercent", infra.heapUsagePercent());
        jvmMap.put("nonHeapUsedMb", infra.nonHeapUsedMb());
        jvmMap.put("gcCount", infra.gcCount());
        jvmMap.put("gcTimeMs", infra.gcTimeMs());
        jvmMap.put("threadCount", infra.threadCount());
        jvmMap.put("peakThreadCount", infra.peakThreadCount());
        jvmMap.put("daemonThreadCount", infra.daemonThreadCount());
        jvmMap.put("uptimeSeconds", infra.uptimeSeconds());
        jvmMap.put("javaVersion", System.getProperty("java.version", "17"));
        jvmMap.put("osName", System.getProperty("os.name", "Unknown"));
        jvmMap.put("osArch", System.getProperty("os.arch", "Unknown"));

        Map<String, Object> diskMap = new LinkedHashMap<>();
        diskMap.put("totalMb", infra.diskTotalMb());
        diskMap.put("usedMb", infra.diskUsedMb());
        diskMap.put("freeMb", infra.diskFreeMb());
        diskMap.put("usagePercent", infra.diskUsagePercent());
        diskMap.put("lowSpaceAlert", infra.diskUsagePercent() >= 85.0);

        Map<String, Object> networkMap = new LinkedHashMap<>();
        networkMap.put("bytesReceived", httpSnap.bytesIn());
        networkMap.put("bytesSent", httpSnap.bytesOut());
        networkMap.put("requestsPerMinute", httpSnap.requestsPerMinute());
        networkMap.put("avgLatencyMs", httpSnap.avgLatencyMs());
        networkMap.put("httpErrors5xx", httpSnap.status5xx());
        networkMap.put("activeSseConnections", realtimeEmitterService.getActiveConnectionsCount());
        networkMap.put("connectedUsers", realtimeEmitterService.getConnectedUsersCount());
        networkMap.put("osNicCountersAvailable", false);

        Map<String, Object> infrastructure = new LinkedHashMap<>();
        infrastructure.put("cpu", cpuMap);
        infrastructure.put("ram", ramMap);
        infrastructure.put("jvm", jvmMap);
        infrastructure.put("disk", diskMap);
        infrastructure.put("network", networkMap);

        Map<String, Object> httpMap = new LinkedHashMap<>();
        httpMap.put("totalRequests", httpSnap.totalRequests());
        httpMap.put("requestsPerMinute", httpSnap.requestsPerMinute());
        httpMap.put("inFlightRequests", httpSnap.inFlightRequests());
        httpMap.put("status2xx", httpSnap.status2xx());
        httpMap.put("status3xx", httpSnap.status3xx());
        httpMap.put("status4xx", httpSnap.status4xx());
        httpMap.put("status5xx", httpSnap.status5xx());
        httpMap.put("recent5xxCount", httpSnap.recent5xxCount());
        httpMap.put("successRatePercent", httpSnap.successRatePercent());
        httpMap.put("avgLatencyMs", httpSnap.avgLatencyMs());
        httpMap.put("p50LatencyMs", httpSnap.p50LatencyMs());
        httpMap.put("p95LatencyMs", httpSnap.p95LatencyMs());
        httpMap.put("p99LatencyMs", httpSnap.p99LatencyMs());
        httpMap.put("maxLatencyMs", httpSnap.maxLatencyMs());
        httpMap.put("latencyHistogram", httpSnap.latencyHistogram());

        Map<String, Object> application = new LinkedHashMap<>();
        application.put("http", httpMap);
        application.put("topEndpoints", compact ? List.of() : httpTelemetryFilter.getTopEndpoints(8));
        application.put("slowestEndpoints", compact ? List.of() : httpTelemetryFilter.getSlowestEndpoints(8));
        application.put("errorEndpoints", compact ? List.of() : httpTelemetryFilter.getErrorEndpoints(8));
        application.put("slowOperations", compact ? List.of() : httpTelemetryFilter.getSlowOperations(10));
        application.put("realtime", Map.of(
                "activeConnections", realtimeEmitterService.getActiveConnectionsCount(),
                "connectedUsers", realtimeEmitterService.getConnectedUsersCount(),
                "connectedCondominiums", realtimeEmitterService.getConnectedCondominiumsCount(),
                "adminTelemetryStreams", adminEmitters.size()
        ));
        application.put("operationalCounters", Map.of(
                "loginSuccess", operationalMetricsService.getLoginSuccessCount(),
                "loginFailure", operationalMetricsService.getLoginFailureCount(),
                "reservationCreated", operationalMetricsService.getReservationCreatedCount(),
                "reservationConflict", operationalMetricsService.getReservationConflictCount(),
                "packageReceived", operationalMetricsService.getPackageReceivedCount(),
                "packageDelivered", operationalMetricsService.getPackageDeliveredCount(),
                "emergencyTriggered", operationalMetricsService.getEmergencyTriggeredCount(),
                "visitorCheckIn", operationalMetricsService.getVisitorCheckInCount()
        ));
        application.put("caches", compact ? List.of() : readCacheStats());

        List<Map<String, Object>> seriesPayload = downsample(windowPoints, 60).stream()
                .map(TelemetryPoint::toMap)
                .toList();

        List<Map<String, Object>> activeAlerts = getActiveAlerts();
        Map<String, Object> alertsSummary = getAlertsSummary();

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("databaseEngine", dbSummary.engine());
        capabilities.put("databaseSizeAvailable", dbSummary.databaseSizeAvailable());
        capabilities.put("hikariPoolAvailable", dbSummary.poolAvailable());
        capabilities.put("osRamAvailable", infra.ramTotalMb() > 0);
        capabilities.put("osNicCountersAvailable", false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", now.toString());
        response.put("window", normalizeWindow(window));
        response.put("overallStatus", computeOverallStatus(dbSummary.up(), infra.heapUsagePercent(), activeAlerts));
        response.put("infrastructure", infrastructure);
        response.put("application", application);
        response.put("database", dbSummary.toMap());
        response.put("timeSeries", seriesPayload);
        response.put("activeAlerts", activeAlerts);
        response.put("alertsSummary", alertsSummary);
        response.put("capabilities", capabilities);
        return response;
    }

    public List<Map<String, Object>> getActiveAlerts() {
        return activeAlertsByRule.values().stream()
                .sorted(Comparator.comparing(AlertRecord::triggeredAt).reversed())
                .map(AlertRecord::toMap)
                .toList();
    }

    public List<Map<String, Object>> getAlertHistory() {
        synchronized (alertLock) {
            List<Map<String, Object>> list = new ArrayList<>();
            Iterator<AlertRecord> it = alertHistory.descendingIterator();
            while (it.hasNext()) {
                list.add(it.next().toMap());
            }
            return list;
        }
    }

    public Map<String, Object> getAlertsSummary() {
        int critical = 0;
        int warning = 0;
        int info = 0;
        int acknowledged = 0;
        for (AlertRecord a : activeAlertsByRule.values()) {
            if ("CRITICAL".equals(a.severity())) critical++;
            else if ("WARNING".equals(a.severity())) warning++;
            else info++;
            if ("ACKNOWLEDGED".equals(a.status())) acknowledged++;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalActive", activeAlertsByRule.size());
        map.put("critical", critical);
        map.put("warning", warning);
        map.put("info", info);
        map.put("acknowledged", acknowledged);
        map.put("historyTotal", alertHistory.size());
        return map;
    }

    public Map<String, Object> acknowledgeAlert(String alertId, String actor) {
        for (Map.Entry<String, AlertRecord> entry : activeAlertsByRule.entrySet()) {
            AlertRecord cur = entry.getValue();
            if (cur.id().equals(alertId)) {
                AlertRecord updated = cur.withStatus("ACKNOWLEDGED", actor, Instant.now().toString());
                activeAlertsByRule.put(entry.getKey(), updated);
                appendHistory(updated);
                auditLogService.logAdminOperation(
                        actor,
                        "ALERT_ACKNOWLEDGED",
                        "SYSTEM_ALERT",
                        alertId,
                        null,
                        null,
                        true,
                        "Alerta " + cur.ruleCode() + " reconhecido por " + actor
                );
                return updated.toMap();
            }
        }
        return null;
    }

    public Map<String, Object> resolveAlert(String alertId, String actor) {
        for (Map.Entry<String, AlertRecord> entry : activeAlertsByRule.entrySet()) {
            AlertRecord cur = entry.getValue();
            if (cur.id().equals(alertId)) {
                AlertRecord resolved = cur.withStatus("RESOLVED", actor, Instant.now().toString());
                activeAlertsByRule.remove(entry.getKey());
                appendHistory(resolved);
                auditLogService.logAdminOperation(
                        actor,
                        "ALERT_RESOLVED",
                        "SYSTEM_ALERT",
                        alertId,
                        null,
                        null,
                        true,
                        "Alerta " + cur.ruleCode() + " resolvido manualmente por " + actor
                );
                return resolved.toMap();
            }
        }
        return null;
    }

    private void collectSample() {
        Instant now = Instant.now();
        InfrastructureReadout infra = readInfrastructure();
        HttpTelemetryFilter.HttpSnapshot httpSnap = httpTelemetryFilter.getSnapshot();
        DatabaseSummary dbSummary = readDatabaseSummary(false);

        TelemetryPoint point = toTelemetryPoint(now, infra, httpSnap, dbSummary);
        synchronized (historyLock) {
            if (history.size() >= MAX_HISTORY_POINTS) {
                history.removeFirst();
            }
            history.addLast(point);
        }

        evaluateAlerts(now, infra, httpSnap, dbSummary);
    }

    private TelemetryPoint toTelemetryPoint(
            Instant now,
            InfrastructureReadout infra,
            HttpTelemetryFilter.HttpSnapshot httpSnap,
            DatabaseSummary dbSummary
    ) {
        return new TelemetryPoint(
                now,
                infra.systemCpuPercent(),
                infra.processCpuPercent(),
                infra.ramUsagePercent(),
                infra.heapUsagePercent(),
                infra.heapUsedMb(),
                infra.diskUsagePercent(),
                httpSnap.requestsPerMinute(),
                httpSnap.avgLatencyMs(),
                httpSnap.p95LatencyMs(),
                httpSnap.p99LatencyMs(),
                httpSnap.status4xx(),
                httpSnap.status5xx(),
                dbSummary.activeConnections(),
                dbSummary.latencyMs(),
                realtimeEmitterService.getActiveConnectionsCount()
        );
    }

    private void evaluateAlerts(
            Instant now,
            InfrastructureReadout infra,
            HttpTelemetryFilter.HttpSnapshot httpSnap,
            DatabaseSummary dbSummary
    ) {
        // CPU
        if (infra.systemCpuPercent() >= 92.0) {
            triggerAlert("HIGH_CPU", "CRITICAL", "CPU do servidor em nível crítico (" + infra.systemCpuPercent() + "%)",
                    infra.systemCpuPercent() + "%", "92%", now);
        } else if (infra.systemCpuPercent() >= 80.0) {
            triggerAlert("HIGH_CPU", "WARNING", "Utilização elevada de CPU (" + infra.systemCpuPercent() + "%)",
                    infra.systemCpuPercent() + "%", "80%", now);
        } else {
            autoResolveRule("HIGH_CPU", now);
        }

        // Heap JVM
        if (infra.heapUsagePercent() >= 92.0) {
            triggerAlert("HIGH_JVM_HEAP", "CRITICAL", "Memória JVM Heap em nível crítico (" + infra.heapUsagePercent() + "%)",
                    infra.heapUsagePercent() + "%", "92%", now);
        } else if (infra.heapUsagePercent() >= 80.0) {
            triggerAlert("HIGH_JVM_HEAP", "WARNING", "Consumo elevado de JVM Heap (" + infra.heapUsagePercent() + "%)",
                    infra.heapUsagePercent() + "%", "80%", now);
        } else {
            autoResolveRule("HIGH_JVM_HEAP", now);
        }

        // RAM Física
        if (infra.ramTotalMb() > 0) {
            if (infra.ramUsagePercent() >= 95.0) {
                triggerAlert("HIGH_SYSTEM_RAM", "CRITICAL", "Memória RAM física próxima da saturação (" + infra.ramUsagePercent() + "%)",
                        infra.ramUsagePercent() + "%", "95%", now);
            } else if (infra.ramUsagePercent() >= 85.0) {
                triggerAlert("HIGH_SYSTEM_RAM", "WARNING", "Uso elevado de memória RAM (" + infra.ramUsagePercent() + "%)",
                        infra.ramUsagePercent() + "%", "85%", now);
            } else {
                autoResolveRule("HIGH_SYSTEM_RAM", now);
            }
        }

        // Disco
        if (infra.diskUsagePercent() >= 95.0) {
            triggerAlert("LOW_DISK_SPACE", "CRITICAL", "Espaço em disco criticamente baixo (" + infra.diskUsagePercent() + "% ocupado)",
                    infra.diskUsagePercent() + "%", "95%", now);
        } else if (infra.diskUsagePercent() >= 85.0) {
            triggerAlert("LOW_DISK_SPACE", "WARNING", "Pouco espaço em disco disponível (" + infra.diskUsagePercent() + "% ocupado)",
                    infra.diskUsagePercent() + "%", "85%", now);
        } else {
            autoResolveRule("LOW_DISK_SPACE", now);
        }

        // Erros HTTP 5xx
        if (httpSnap.recent5xxCount() >= 5) {
            triggerAlert("HTTP_5XX_SPIKE", "CRITICAL", "Pico de erros HTTP 5xx no último minuto (" + httpSnap.recent5xxCount() + " erros)",
                    httpSnap.recent5xxCount() + " erros/min", "5 erros/min", now);
        } else {
            autoResolveRule("HTTP_5XX_SPIKE", now);
        }

        // Latência p95
        if (httpSnap.p95LatencyMs() >= 1200) {
            triggerAlert("HIGH_LATENCY_P95", "CRITICAL", "Latência p95 crítica (" + httpSnap.p95LatencyMs() + " ms)",
                    httpSnap.p95LatencyMs() + " ms", "1200 ms", now);
        } else if (httpSnap.p95LatencyMs() >= 500) {
            triggerAlert("HIGH_LATENCY_P95", "WARNING", "Latência p95 elevada (" + httpSnap.p95LatencyMs() + " ms)",
                    httpSnap.p95LatencyMs() + " ms", "500 ms", now);
        } else {
            autoResolveRule("HIGH_LATENCY_P95", now);
        }

        // Banco de dados
        if (!dbSummary.up()) {
            triggerAlert("DB_UNAVAILABLE", "CRITICAL", "Banco de dados inacessível ao ping de verificação",
                    "DOWN", "UP", now);
        } else {
            autoResolveRule("DB_UNAVAILABLE", now);
        }

        if (dbSummary.poolAvailable() && dbSummary.poolUsagePercent() >= 80.0) {
            triggerAlert("DB_POOL_SATURATION", "WARNING", "Pool de conexões do banco com alta saturação (" + dbSummary.poolUsagePercent() + "%)",
                    dbSummary.poolUsagePercent() + "%", "80%", now);
        } else {
            autoResolveRule("DB_POOL_SATURATION", now);
        }

        // Anomalia de falhas de login
        long curFail = operationalMetricsService.getLoginFailureCount();
        long deltaFail = curFail - lastAlertLoginFailures.getAndSet(curFail);
        if (deltaFail >= 10) {
            triggerAlert("AUTH_FAILURE_ANOMALY", "WARNING", "Múltiplas falhas de autenticação detectadas no intervalo (" + deltaFail + ")",
                    deltaFail + " falhas", "10 falhas", now);
        }
    }

    private void triggerAlert(String ruleCode, String severity, String message, String currentValue, String threshold, Instant now) {
        activeAlertsByRule.compute(ruleCode, (code, existing) -> {
            if (existing == null) {
                AlertRecord created = new AlertRecord(
                        UUID.randomUUID().toString().substring(0, 8),
                        ruleCode,
                        severity,
                        "ACTIVE",
                        message,
                        currentValue,
                        threshold,
                        now.toString(),
                        null,
                        null
                );
                appendHistory(created);
                return created;
            }
            if ("ACKNOWLEDGED".equals(existing.status())) {
                return new AlertRecord(
                        existing.id(),
                        ruleCode,
                        severity,
                        "ACKNOWLEDGED",
                        message,
                        currentValue,
                        threshold,
                        existing.triggeredAt(),
                        existing.actor(),
                        existing.updatedAt()
                );
            }
            return new AlertRecord(
                    existing.id(),
                    ruleCode,
                    severity,
                    "ACTIVE",
                    message,
                    currentValue,
                    threshold,
                    existing.triggeredAt(),
                    null,
                    now.toString()
            );
        });
    }

    private void autoResolveRule(String ruleCode, Instant now) {
        AlertRecord existing = activeAlertsByRule.remove(ruleCode);
        if (existing != null) {
            appendHistory(existing.withStatus("RESOLVED", "SYSTEM", now.toString()));
        }
    }

    private void appendHistory(AlertRecord record) {
        synchronized (alertLock) {
            if (alertHistory.size() >= MAX_ALERT_HISTORY) {
                alertHistory.removeFirst();
            }
            alertHistory.addLast(record);
        }
    }

    private InfrastructureReadout readInfrastructure() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsedMb = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMaxRaw = memoryBean.getHeapMemoryUsage().getMax();
        long heapMaxMb = heapMaxRaw > 0 ? heapMaxRaw / (1024 * 1024) : 0L;
        long heapCommittedMb = memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
        long nonHeapUsedMb = memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
        double heapUsagePercent = heapMaxMb > 0 ? round1(((double) heapUsedMb / heapMaxMb) * 100.0) : 0.0;

        long gcCount = 0;
        long gcTimeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() > 0) gcCount += gc.getCollectionCount();
            if (gc.getCollectionTime() > 0) gcTimeMs += gc.getCollectionTime();
        }

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        int daemonThreadCount = threadBean.getDaemonThreadCount();

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeSeconds = runtimeBean.getUptime() / 1000L;

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        int processors = osBean.getAvailableProcessors();
        double loadAvg = osBean.getSystemLoadAverage() >= 0 ? round1(osBean.getSystemLoadAverage()) : 0.0;
        double sysCpuPercent = 0.0;
        double procCpuPercent = 0.0;
        long ramTotalMb = 0L;
        long ramFreeMb = 0L;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            double rawSys = sunOs.getCpuLoad();
            if (rawSys >= 0) sysCpuPercent = round1(rawSys * 100.0);
            double rawProc = sunOs.getProcessCpuLoad();
            if (rawProc >= 0) procCpuPercent = round1(rawProc * 100.0);
            long totalBytes = sunOs.getTotalMemorySize();
            long freeBytes = sunOs.getFreeMemorySize();
            if (totalBytes > 0) {
                ramTotalMb = totalBytes / (1024 * 1024);
                ramFreeMb = Math.max(0L, freeBytes / (1024 * 1024));
            }
        }
        long ramUsedMb = Math.max(0L, ramTotalMb - ramFreeMb);
        double ramUsagePercent = ramTotalMb > 0 ? round1(((double) ramUsedMb / ramTotalMb) * 100.0) : 0.0;

        File root = new File(".");
        long diskTotalMb = root.getTotalSpace() / (1024 * 1024);
        long diskFreeMb = root.getFreeSpace() / (1024 * 1024);
        long diskUsedMb = Math.max(0L, diskTotalMb - diskFreeMb);
        double diskUsagePercent = diskTotalMb > 0 ? round1(((double) diskUsedMb / diskTotalMb) * 100.0) : 0.0;

        return new InfrastructureReadout(
                sysCpuPercent,
                procCpuPercent,
                processors,
                loadAvg,
                ramTotalMb,
                ramUsedMb,
                ramFreeMb,
                ramUsagePercent,
                heapUsedMb,
                heapMaxMb,
                heapCommittedMb,
                heapUsagePercent,
                nonHeapUsedMb,
                gcCount,
                gcTimeMs,
                threadCount,
                peakThreadCount,
                daemonThreadCount,
                uptimeSeconds,
                diskTotalMb,
                diskUsedMb,
                diskFreeMb,
                diskUsagePercent
        );
    }

    private DatabaseSummary readDatabaseSummary(boolean compact) {
        long startNs = System.nanoTime();
        boolean up = false;
        String engine = "Unknown";
        try {
            Integer ping = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            up = (ping != null && ping == 1);
        } catch (Exception e) {
            dbConnectionErrors.increment();
        }
        long latencyMs = Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);

        int activeConns = 0;
        int idleConns = 0;
        int totalConns = 0;
        int awaitingConns = 0;
        int maxPoolSize = 0;
        boolean poolAvailable = false;

        try {
            HikariDataSource hikari = dataSource.isWrapperFor(HikariDataSource.class)
                    ? dataSource.unwrap(HikariDataSource.class)
                    : (dataSource instanceof HikariDataSource h ? h : null);
            if (hikari != null) {
                maxPoolSize = hikari.getMaximumPoolSize();
                HikariPoolMXBean poolBean = hikari.getHikariPoolMXBean();
                if (poolBean != null) {
                    activeConns = poolBean.getActiveConnections();
                    idleConns = poolBean.getIdleConnections();
                    totalConns = poolBean.getTotalConnections();
                    awaitingConns = poolBean.getThreadsAwaitingConnection();
                    poolAvailable = true;
                }
            }
        } catch (Exception ignored) {
        }

        double poolUsagePercent = maxPoolSize > 0
                ? round1(((double) activeConns / maxPoolSize) * 100.0)
                : 0.0;

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            if (meta != null && meta.getDatabaseProductName() != null) {
                engine = meta.getDatabaseProductName();
            }
        } catch (Exception ignored) {
        }

        boolean isPostgres = engine.toLowerCase(Locale.ROOT).contains("postgresql");
        boolean dbSizeAvailable = false;
        double databaseSizeMb = 0.0;
        long commits = 0L;
        long rollbacks = 0L;
        double cacheHitRatio = 0.0;
        long qps = lastQps.get();
        List<Map<String, Object>> topTables = List.of();
        List<Map<String, Object>> slowQueries = List.of();

        if (isPostgres && up) {
            try {
                Long sizeBytes = jdbcTemplate.queryForObject("SELECT pg_database_size(current_database())", Long.class);
                if (sizeBytes != null) {
                    databaseSizeMb = round1(sizeBytes / (1024.0 * 1024.0));
                    dbSizeAvailable = true;
                }
                Map<String, Object> dbStats = jdbcTemplate.queryForMap(
                        "SELECT xact_commit, xact_rollback, blks_hit, blks_read FROM pg_stat_database WHERE datname = current_database()"
                );
                commits = ((Number) dbStats.getOrDefault("xact_commit", 0)).longValue();
                rollbacks = ((Number) dbStats.getOrDefault("xact_rollback", 0)).longValue();
                long hit = ((Number) dbStats.getOrDefault("blks_hit", 0)).longValue();
                long read = ((Number) dbStats.getOrDefault("blks_read", 0)).longValue();
                if (hit + read > 0) {
                    cacheHitRatio = round1(((double) hit / (hit + read)) * 100.0);
                }

                long totalTx = commits + rollbacks;
                long nowMs = System.currentTimeMillis();
                long prevTx = lastTxCount.getAndSet(totalTx);
                long prevMs = lastTxSampleMs.getAndSet(nowMs);
                if (prevTx >= 0 && prevMs > 0 && nowMs > prevMs) {
                    long deltaTx = Math.max(0L, totalTx - prevTx);
                    double elapsedSec = (nowMs - prevMs) / 1000.0;
                    qps = Math.round(deltaTx / elapsedSec);
                    lastQps.set(qps);
                }

                if (!compact) {
                    topTables = jdbcTemplate.queryForList(
                            "SELECT relname AS table_name, n_live_tup AS row_count, " +
                                    "ROUND(pg_total_relation_size(relid) / 1048576.0, 2) AS size_mb " +
                                    "FROM pg_stat_user_tables ORDER BY n_live_tup DESC LIMIT 12"
                    );
                    slowQueries = jdbcTemplate.queryForList(
                            "SELECT pid, state, wait_event_type AS wait_event, " +
                                    "ROUND((EXTRACT(EPOCH FROM (now() - query_start)) * 1000)::numeric, 1) AS duration_ms, " +
                                    "left(regexp_replace(query, '\\s+', ' ', 'g'), 120) AS query_preview " +
                                    "FROM pg_stat_activity " +
                                    "WHERE state = 'active' AND query_start IS NOT NULL " +
                                    "AND pid <> pg_backend_pid() " +
                                    "AND now() - query_start > interval '300 milliseconds' " +
                                    "ORDER BY duration_ms DESC LIMIT 10"
                    );
                }
            } catch (Exception e) {
                log.debug("Leitura de estatísticas PostgreSQL parcial: {}", e.getMessage());
            }
        } else if (up && !compact) {
            topTables = readPortableTableCounts();
        }

        return new DatabaseSummary(
                up,
                engine,
                latencyMs,
                dbConnectionErrors.sum(),
                poolAvailable,
                activeConns,
                idleConns,
                totalConns,
                awaitingConns,
                maxPoolSize,
                poolUsagePercent,
                dbSizeAvailable,
                databaseSizeMb,
                commits,
                rollbacks,
                cacheHitRatio,
                qps,
                topTables,
                slowQueries
        );
    }

    private List<Map<String, Object>> readPortableTableCounts() {
        List<String> coreTables = List.of(
                "tb_user", "tb_condominium", "tb_condominium_user", "tb_unit",
                "tb_package", "tb_reservation", "tb_occurrence_ticket",
                "tb_visitor_access", "tb_notification", "tb_audit_log"
        );
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String table : coreTables) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                if (count != null) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("table_name", table);
                    item.put("row_count", count);
                    item.put("size_mb", null);
                    rows.add(item);
                }
            } catch (Exception ignored) {
            }
        }
        rows.sort(Comparator.<Map<String, Object>, Long>comparing(m -> ((Number) m.get("row_count")).longValue()).reversed());
        return rows;
    }

    private List<Map<String, Object>> readCacheStats() {
        if (cacheManager == null) return List.of();
        List<Map<String, Object>> list = new ArrayList<>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            int size = 0;
            if (cache != null && cache.getNativeCache() instanceof Map<?, ?> map) {
                size = map.size();
            }
            list.add(Map.of(
                    "name", name,
                    "entries", size,
                    "provider", cache != null ? cache.getClass().getSimpleName() : "Cache"
            ));
        }
        return list;
    }

    private List<TelemetryPoint> getPointsForWindow(String window) {
        Duration span = switch (normalizeWindow(window)) {
            case "1h" -> Duration.ofHours(1);
            case "6h" -> Duration.ofHours(6);
            case "24h" -> Duration.ofHours(24);
            default -> Duration.ofMinutes(15);
        };
        Instant cutoff = Instant.now().minus(span);
        synchronized (historyLock) {
            List<TelemetryPoint> result = new ArrayList<>();
            for (TelemetryPoint p : history) {
                if (!p.timestamp().isBefore(cutoff)) {
                    result.add(p);
                }
            }
            if (result.isEmpty() && !history.isEmpty()) {
                result.add(history.peekLast());
            }
            return result;
        }
    }

    private String normalizeWindow(String window) {
        if (window == null) return "15m";
        return switch (window.trim().toLowerCase(Locale.ROOT)) {
            case "1h", "6h", "24h" -> window.trim().toLowerCase(Locale.ROOT);
            default -> "15m";
        };
    }

    private List<TelemetryPoint> downsample(List<TelemetryPoint> input, int maxPoints) {
        if (input.size() <= maxPoints) return input;
        List<TelemetryPoint> sampled = new ArrayList<>(maxPoints);
        double step = (double) (input.size() - 1) / (maxPoints - 1);
        for (int i = 0; i < maxPoints; i++) {
            int idx = (int) Math.round(i * step);
            sampled.add(input.get(Math.min(idx, input.size() - 1)));
        }
        return sampled;
    }

    private String computeOverallStatus(boolean dbUp, double heapPercent, List<Map<String, Object>> activeAlerts) {
        if (!dbUp || heapPercent >= 98.0) return "DOWN";
        boolean hasCritical = activeAlerts.stream().anyMatch(a -> "CRITICAL".equals(a.get("severity")));
        if (hasCritical) return "DEGRADED";
        if (!activeAlerts.isEmpty()) return "WARN";
        return "UP";
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record InfrastructureReadout(
            double systemCpuPercent,
            double processCpuPercent,
            int availableProcessors,
            double systemLoadAverage,
            long ramTotalMb,
            long ramUsedMb,
            long ramFreeMb,
            double ramUsagePercent,
            long heapUsedMb,
            long heapMaxMb,
            long heapCommittedMb,
            double heapUsagePercent,
            long nonHeapUsedMb,
            long gcCount,
            long gcTimeMs,
            int threadCount,
            int peakThreadCount,
            int daemonThreadCount,
            long uptimeSeconds,
            long diskTotalMb,
            long diskUsedMb,
            long diskFreeMb,
            double diskUsagePercent
    ) {}

    private record DatabaseSummary(
            boolean up,
            String engine,
            long latencyMs,
            long connectionErrors,
            boolean poolAvailable,
            int activeConnections,
            int idleConnections,
            int totalConnections,
            int awaitingConnections,
            int maxPoolSize,
            double poolUsagePercent,
            boolean databaseSizeAvailable,
            double databaseSizeMb,
            long commits,
            long rollbacks,
            double cacheHitRatioPercent,
            long queriesPerSecond,
            List<Map<String, Object>> topTables,
            List<Map<String, Object>> slowQueries
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", up ? "UP" : "DOWN");
            map.put("engine", engine);
            map.put("latencyMs", latencyMs);
            map.put("connectionErrors", connectionErrors);
            map.put("poolAvailable", poolAvailable);
            map.put("activeConnections", activeConnections);
            map.put("idleConnections", idleConnections);
            map.put("totalConnections", totalConnections);
            map.put("availableConnections", Math.max(0, maxPoolSize - activeConnections));
            map.put("awaitingConnections", awaitingConnections);
            map.put("maxPoolSize", maxPoolSize);
            map.put("poolUsagePercent", poolUsagePercent);
            map.put("databaseSizeAvailable", databaseSizeAvailable);
            map.put("databaseSizeMb", databaseSizeMb);
            map.put("commits", commits);
            map.put("rollbacks", rollbacks);
            map.put("cacheHitRatioPercent", cacheHitRatioPercent);
            map.put("queriesPerSecond", queriesPerSecond);
            map.put("topTables", topTables);
            map.put("slowQueries", slowQueries);
            return map;
        }
    }

    private record TelemetryPoint(
            Instant timestamp,
            double systemCpuPercent,
            double processCpuPercent,
            double ramUsagePercent,
            double heapUsagePercent,
            long heapUsedMb,
            double diskUsagePercent,
            long requestsPerMinute,
            double avgLatencyMs,
            long p95LatencyMs,
            long p99LatencyMs,
            long status4xx,
            long status5xx,
            int dbActiveConnections,
            long dbLatencyMs,
            int activeSseConnections
    ) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("timestamp", timestamp.toString());
            map.put("systemCpuPercent", systemCpuPercent);
            map.put("processCpuPercent", processCpuPercent);
            map.put("ramUsagePercent", ramUsagePercent);
            map.put("heapUsagePercent", heapUsagePercent);
            map.put("heapUsedMb", heapUsedMb);
            map.put("diskUsagePercent", diskUsagePercent);
            map.put("requestsPerMinute", requestsPerMinute);
            map.put("avgLatencyMs", avgLatencyMs);
            map.put("p95LatencyMs", p95LatencyMs);
            map.put("p99LatencyMs", p99LatencyMs);
            map.put("status4xx", status4xx);
            map.put("status5xx", status5xx);
            map.put("dbActiveConnections", dbActiveConnections);
            map.put("dbLatencyMs", dbLatencyMs);
            map.put("activeSseConnections", activeSseConnections);
            return map;
        }
    }

    private record AlertRecord(
            String id,
            String ruleCode,
            String severity,
            String status,
            String message,
            String currentValue,
            String threshold,
            String triggeredAt,
            String actor,
            String updatedAt
    ) {
        AlertRecord withStatus(String newStatus, String actorName, String timestamp) {
            return new AlertRecord(id, ruleCode, severity, newStatus, message, currentValue, threshold, triggeredAt, actorName, timestamp);
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("ruleCode", ruleCode);
            map.put("severity", severity);
            map.put("status", status);
            map.put("message", message);
            map.put("currentValue", currentValue);
            map.put("threshold", threshold);
            map.put("triggeredAt", triggeredAt);
            map.put("actor", actor);
            map.put("updatedAt", updatedAt);
            return map;
        }
    }
}
