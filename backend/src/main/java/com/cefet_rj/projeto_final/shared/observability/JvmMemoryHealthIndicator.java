package com.cefet_rj.projeto_final.shared.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Exposes JVM heap usage under Actuator health components ({@code components.jvmMemory}).
 */
@Component("jvmMemory")
public class JvmMemoryHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long heapUsedMb = heapUsed / (1024 * 1024);
        long heapMaxMb = heapMax > 0 ? heapMax / (1024 * 1024) : -1;
        boolean healthy = heapMax <= 0 || (((double) heapUsed / heapMax) * 100.0) < 98.0;

        Health.Builder builder = healthy ? Health.up() : Health.outOfService();
        return builder
                .withDetail("usedMb", heapUsedMb)
                .withDetail("maxMb", heapMaxMb)
                .build();
    }
}
