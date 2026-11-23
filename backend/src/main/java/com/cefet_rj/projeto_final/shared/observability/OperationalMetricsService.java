package com.cefet_rj.projeto_final.shared.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final LongAdder auditWriteFailureCounter = new LongAdder();

    private Counter loginSuccessMicrometer;
    private Counter loginFailureMicrometer;
    private Counter reservationCreatedMicrometer;
    private Counter reservationConflictMicrometer;
    private Counter packageReceivedMicrometer;
    private Counter packageDeliveredMicrometer;
    private Counter emergencyTriggeredMicrometer;
    private Counter emergencyResolvedMicrometer;
    private Counter visitorCheckInMicrometer;
    private Counter auditWriteFailureMicrometer;

    @Autowired(required = false)
    public void bindMeterRegistry(MeterRegistry meterRegistry) {
        if (meterRegistry == null) {
            return;
        }
        this.loginSuccessMicrometer = Counter.builder("condo.auth.login.success")
                .description("Number of successful authentications")
                .register(meterRegistry);
        this.loginFailureMicrometer = Counter.builder("condo.auth.login.failure")
                .description("Number of failed authentications")
                .register(meterRegistry);
        this.reservationCreatedMicrometer = Counter.builder("condo.reservation.created")
                .description("Number of reservations created")
                .register(meterRegistry);
        this.reservationConflictMicrometer = Counter.builder("condo.reservation.conflict")
                .description("Number of reservation scheduling conflicts")
                .register(meterRegistry);
        this.packageReceivedMicrometer = Counter.builder("condo.package.received")
                .description("Number of packages received")
                .register(meterRegistry);
        this.packageDeliveredMicrometer = Counter.builder("condo.package.delivered")
                .description("Number of packages delivered to residents")
                .register(meterRegistry);
        this.emergencyTriggeredMicrometer = Counter.builder("condo.emergency.triggered")
                .description("Number of emergency alerts triggered")
                .register(meterRegistry);
        this.emergencyResolvedMicrometer = Counter.builder("condo.emergency.resolved")
                .description("Number of emergency alerts resolved")
                .register(meterRegistry);
        this.visitorCheckInMicrometer = Counter.builder("condo.visitor.checkin")
                .description("Number of visitor check-ins")
                .register(meterRegistry);
        this.auditWriteFailureMicrometer = Counter.builder("condo.audit.write.failure")
                .description("Number of failed audit log persistence attempts")
                .register(meterRegistry);

        Gauge.builder("condo.jvm.heap.used", Runtime.getRuntime(), r -> (double) (r.totalMemory() - r.freeMemory()))
                .description("JVM heap memory used in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);
    }

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
        if (loginSuccessMicrometer != null) loginSuccessMicrometer.increment();
    }

    public void recordLoginFailure(String reason) {
        String safeReason = (reason != null && !reason.isBlank()) ? reason : "unknown";
        loginFailureCounters.computeIfAbsent(safeReason, r -> new LongAdder()).increment();
        if (loginFailureMicrometer != null) loginFailureMicrometer.increment();
    }

    public void recordReservationCreated() {
        reservationCreatedCounter.increment();
        if (reservationCreatedMicrometer != null) reservationCreatedMicrometer.increment();
    }

    public void recordReservationConflict() {
        reservationConflictCounter.increment();
        if (reservationConflictMicrometer != null) reservationConflictMicrometer.increment();
    }

    public void recordPackageReceived() {
        packageReceivedCounter.increment();
        if (packageReceivedMicrometer != null) packageReceivedMicrometer.increment();
    }

    public void recordPackageDelivered() {
        packageDeliveredCounter.increment();
        if (packageDeliveredMicrometer != null) packageDeliveredMicrometer.increment();
    }

    public void recordEmergencyTriggered(String severity) {
        String safeSeverity = (severity != null && !severity.isBlank()) ? severity.toUpperCase() : "GENERAL";
        emergencyTriggeredCounters.computeIfAbsent(safeSeverity, s -> new LongAdder()).increment();
        if (emergencyTriggeredMicrometer != null) emergencyTriggeredMicrometer.increment();
    }

    public void recordEmergencyResolved() {
        emergencyResolvedCounter.increment();
        if (emergencyResolvedMicrometer != null) emergencyResolvedMicrometer.increment();
    }

    public void recordVisitorCheckIn() {
        visitorCheckInCounter.increment();
        if (visitorCheckInMicrometer != null) visitorCheckInMicrometer.increment();
    }

    public void recordAuditWriteFailure() {
        auditWriteFailureCounter.increment();
        if (auditWriteFailureMicrometer != null) auditWriteFailureMicrometer.increment();
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

    public long getAuditWriteFailureCount() {
        return auditWriteFailureCounter.sum();
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
                "condo.audit.write.failure",
                "jvm.memory.used",
                "jvm.memory.max"
        );
    }
}
