package com.cefet_rj.projeto_final.shared.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/actuator")
public class ActuatorObservabilityController {

    private static final Logger log = LoggerFactory.getLogger(ActuatorObservabilityController.class);

    private final JdbcTemplate jdbcTemplate;
    private final OperationalMetricsService metricsService;

    @Value("${spring.application.name:projeto-final}")
    private String appName;

    public ActuatorObservabilityController(JdbcTemplate jdbcTemplate, OperationalMetricsService metricsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> discovery() {
        Map<String, Object> links = new LinkedHashMap<>();
        links.put("self", Map.of("href", "/actuator"));
        links.put("health", Map.of("href", "/actuator/health"));
        links.put("info", Map.of("href", "/actuator/info"));
        links.put("metrics", Map.of("href", "/actuator/metrics"));
        return ResponseEntity.ok(Map.of("_links", links));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbOk = checkDatabase();
        boolean memoryOk = checkMemory();
        boolean diskOk = checkDisk();

        boolean allOk = dbOk && memoryOk && diskOk;
        String overallStatus = allOk ? "UP" : "DOWN";

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("db", Map.of(
                "status", dbOk ? "UP" : "DOWN",
                "details", Map.of("database", dbOk ? "UP" : "UNREACHABLE", "validationQuery", "SELECT 1")
        ));

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsedMb = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMaxMb = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        components.put("jvmMemory", Map.of(
                "status", memoryOk ? "UP" : "DEGRADED",
                "details", Map.of("usedMb", heapUsedMb, "maxMb", heapMaxMb)
        ));

        File root = new File(".");
        long freeDiskMb = root.getFreeSpace() / (1024 * 1024);
        long totalDiskMb = root.getTotalSpace() / (1024 * 1024);
        components.put("diskSpace", Map.of(
                "status", diskOk ? "UP" : "DOWN",
                "details", Map.of("freeMb", freeDiskMb, "totalMb", totalDiskMb, "thresholdMb", 50)
        ));

        components.put("ping", Map.of("status", "UP"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", overallStatus);
        response.put("components", components);

        return ResponseEntity.status(allOk ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        boolean ready = checkDatabase();
        return ResponseEntity
                .status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", ready ? "UP" : "OUT_OF_SERVICE"));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("app", Map.of(
                "name", appName,
                "description", "Condominium Management Platform API",
                "version", "1.0.0"
        ));
        info.put("java", Map.of(
                "version", System.getProperty("java.version"),
                "vendor", System.getProperty("java.vendor")
        ));
        return ResponseEntity.ok(info);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> listMetrics() {
        return ResponseEntity.ok(Map.of("names", metricsService.getAllMetricNames()));
    }

    @GetMapping("/metrics/{metricName}")
    public ResponseEntity<Map<String, Object>> getMetric(@PathVariable String metricName) {
        Map<String, Object> metric = metricsService.getMetricDetails(metricName);
        if (metric == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metric);
    }

    private boolean checkDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception ex) {
            log.error("Database health check ping failed: {}", ex.getMessage());
            return false;
        }
    }

    private boolean checkMemory() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            if (heapMax > 0) {
                double percent = ((double) heapUsed / heapMax) * 100.0;
                return percent < 98.0;
            }
            return true;
        } catch (Exception ex) {
            return true;
        }
    }

    private boolean checkDisk() {
        try {
            File root = new File(".");
            long freeSpace = root.getFreeSpace();
            return freeSpace == 0 || freeSpace >= (50L * 1024 * 1024);
        } catch (Exception ex) {
            return true;
        }
    }
}
