package com.cefet_rj.projeto_final.shared.observability;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class OperationalMetricsService {

    private final LongAdder loginSuccessCounter = new LongAdder();
    private final ConcurrentMap<String, LongAdder> loginFailureCounters = new ConcurrentHashMap<>();
    private final LongAdder reservationCreatedCounter = new LongAdder();
    private final LongAdder reservationConflictCounter = new LongAdder();
    private final LongAdder packageReceivedCounter = new LongAdder();
    private final LongAdder packageDeliveredCounter = new LongAdder();
    private final ConcurrentMap<String, LongAdder> emergencyTriggeredCounters = new ConcurrentHashMap<>();
    private final LongAdder emergencyResolvedCounter = new LongAdder();
    private final LongAdder visitorCheckInCounter = new LongAdder();

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void recordLoginFailure(String reason) {
        String safeReason = (reason != null && !reason.isBlank()) ? reason : "unknown";
        loginFailureCounters.computeIfAbsent(safeReason, r -> new LongAdder()).increment();
    }

    public void recordReservationCreated() {
        reservationCreatedCounter.increment();
    }

    public void recordReservationConflict() {
        reservationConflictCounter.increment();
    }

    public void recordPackageReceived() {
        packageReceivedCounter.increment();
    }

    public void recordPackageDelivered() {
        packageDeliveredCounter.increment();
    }

    public void recordEmergencyTriggered(String severity) {
        String safeSeverity = (severity != null && !severity.isBlank()) ? severity.toUpperCase() : "GENERAL";
        emergencyTriggeredCounters.computeIfAbsent(safeSeverity, s -> new LongAdder()).increment();
    }

    public void recordEmergencyResolved() {
        emergencyResolvedCounter.increment();
    }

    public void recordVisitorCheckIn() {
        visitorCheckInCounter.increment();
    }

    public long getLoginSuccessCount() {
        return loginSuccessCounter.sum();
    }

    public long getLoginFailureCount() {
        return loginFailureCounters.values().stream().mapToLong(LongAdder::sum).sum();
    }

    public long getReservationCreatedCount() {
        return reservationCreatedCounter.sum();
    }

    public long getReservationConflictCount() {
        return reservationConflictCounter.sum();
    }

    public long getPackageReceivedCount() {
        return packageReceivedCounter.sum();
    }

    public long getPackageDeliveredCount() {
        return packageDeliveredCounter.sum();
    }

    public long getEmergencyTriggeredCount() {
        return emergencyTriggeredCounters.values().stream().mapToLong(LongAdder::sum).sum();
    }

    public long getEmergencyResolvedCount() {
        return emergencyResolvedCounter.sum();
    }

    public long getVisitorCheckInCount() {
        return visitorCheckInCounter.sum();
    }

    public List<String> getAllMetricNames() {
        return List.of(
                "condo.auth.login.success",
                "condo.auth.login.failure",
                "condo.reservation.created",
                "condo.reservation.conflict",
                "condo.package.received",
                "condo.package.delivered",
                "condo.emergency.triggered",
                "condo.emergency.resolved",
                "condo.visitor.checkin",
                "jvm.memory.used",
                "jvm.memory.max"
        );
    }

    public Map<String, Object> getMetricDetails(String metricName) {
        if (metricName == null) return null;
        return switch (metricName) {
            case "condo.auth.login.success" -> createMetricMap("condo.auth.login.success", "Number of successful authentications", getLoginSuccessCount());
            case "condo.auth.login.failure" -> createMetricMap("condo.auth.login.failure", "Number of failed authentications", getLoginFailureCount());
            case "condo.reservation.created" -> createMetricMap("condo.reservation.created", "Number of reservations created", getReservationCreatedCount());
            case "condo.reservation.conflict" -> createMetricMap("condo.reservation.conflict", "Number of reservation scheduling conflicts", getReservationConflictCount());
            case "condo.package.received" -> createMetricMap("condo.package.received", "Number of packages received", getPackageReceivedCount());
            case "condo.package.delivered" -> createMetricMap("condo.package.delivered", "Number of packages delivered to residents", getPackageDeliveredCount());
            case "condo.emergency.triggered" -> createMetricMap("condo.emergency.triggered", "Number of emergency alerts triggered", getEmergencyTriggeredCount());
            case "condo.emergency.resolved" -> createMetricMap("condo.emergency.resolved", "Number of emergency alerts resolved", getEmergencyResolvedCount());
            case "condo.visitor.checkin" -> createMetricMap("condo.visitor.checkin", "Number of visitor check-ins", getVisitorCheckInCount());
            case "jvm.memory.used" -> createMetricMap("jvm.memory.used", "JVM heap memory used in bytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            case "jvm.memory.max" -> createMetricMap("jvm.memory.max", "JVM heap memory maximum in bytes", Runtime.getRuntime().maxMemory());
            default -> null;
        };
    }

    private Map<String, Object> createMetricMap(String name, String description, double value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("description", description);
        result.put("baseUnit", null);
        result.put("measurements", List.of(Map.of("statistic", "VALUE", "value", value)));
        result.put("availableTags", List.of());
        return result;
    }
}
