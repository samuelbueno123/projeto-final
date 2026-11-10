package com.cefet_rj.projeto_final.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuração de Alta Escalabilidade, Caching em Memória e Processamento Assíncrono.
 */
@Configuration
@EnableCaching
@EnableAsync
public class PerformanceAndCacheConfig {

    /**
     * Cache Manager de alta performance para reduzir consultas redundantes ao banco.
     * Caches:
     * - roles: Perfis de autorização (ROLE_ADMIN, ROLE_RESIDENT, etc.)
     * - condoFeatures: Permissões de módulos contratados por condomínio
     * - billingPlans: Catálogo comercial de planos
     * - condominiumDetails: Metadados estruturais do condomínio
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "roles",
                "condoFeatures",
                "billingPlans",
                "condominiumDetails"
        );
    }

    /**
     * Thread Pool dedicado para eventos assíncronos (notificações, SSE e auditoria de fundo).
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-exec-");
        executor.initialize();
        return executor;
    }
}
