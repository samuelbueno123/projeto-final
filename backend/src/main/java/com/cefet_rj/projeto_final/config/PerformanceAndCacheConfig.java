package com.cefet_rj.projeto_final.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.MDC;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Configuração de Alta Escalabilidade, Caching com Caffeine e Processamento Assíncrono.
 */
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
public class PerformanceAndCacheConfig {

    /**
     * Cache Manager limitado em memória e TTL via Caffeine para evitar vazamento de memória
     * e eliminar consultas repetitivas em caminhos quentes (auth, permissões de módulo e planos).
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "userAuth",
                "condoFeatures",
                "billingPlans",
                "roles",
                "condominiumDetails"
        );
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(5000)
                        .recordStats()
        );
        return cacheManager;
    }

    /**
     * Thread Pool dedicado para eventos assíncronos (notificações, SSE e auditoria de fundo)
     * com propagação de MDC, shutdown gracioso e backpressure via CallerRunsPolicy.
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-exec-");
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * Scheduler dedicado para heartbeats SSE e tarefas periódicas de manutenção/expurgo.
     */
    @Bean(name = "sseHeartbeatScheduler")
    public TaskScheduler sseHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("sse-hb-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);
        scheduler.initialize();
        return scheduler;
    }

    private TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                } else {
                    MDC.clear();
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
