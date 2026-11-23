package com.cefet_rj.projeto_final.condominiums.security;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.enums.SubscriptionStatus;
import com.cefet_rj.projeto_final.billing.repositories.CondominiumSubscriptionRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CondominiumFeatureAccessService {

    private final CondominiumRepository condominiumRepository;

    @Autowired(required = false)
    private CondominiumSubscriptionRepository subscriptionRepository;

    @Autowired
    @Lazy
    private CondominiumFeatureAccessService self;

    @Autowired(required = false)
    private CacheManager cacheManager;

    public void validateFeatureEnabled(
            Long condominiumId,
            FeatureCode featureCode
    ) {
        boolean enabled = (self != null ? self : this).hasFeature(condominiumId, featureCode);
        if (!enabled) {
            throw new ForbiddenException(
                    "Recurso " + featureCode
                            + " não está disponível no plano ou assinatura atual do condomínio "
                            + condominiumId
            );
        }
    }

    @Cacheable(value = "condoFeatures", key = "#condominiumId + '-' + #featureCode.name()")
    public boolean hasFeature(
            Long condominiumId,
            FeatureCode featureCode
    ) {
        Condominium condominium = condominiumRepository
                .findById(condominiumId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Condominium not found: " + condominiumId
                        )
                );

        if (!Boolean.TRUE.equals(condominium.getIsActive())) {
            return false;
        }

        BillingPlan effectivePlan = resolveOperationalPlan(condominium);
        if (effectivePlan == null || !Boolean.TRUE.equals(effectivePlan.getActive())) {
            return false;
        }

        return effectivePlan.getFeatures()
                .stream()
                .anyMatch(feature ->
                        feature.getCode()
                                .equalsIgnoreCase(featureCode.name())
                );
    }

    public List<String> getEnabledFeatureCodes(Long condominiumId) {
        Condominium condominium = condominiumRepository
                .findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));
        if (!Boolean.TRUE.equals(condominium.getIsActive())) {
            return Collections.emptyList();
        }
        BillingPlan effectivePlan = resolveOperationalPlan(condominium);
        if (effectivePlan == null || !Boolean.TRUE.equals(effectivePlan.getActive())) {
            return Collections.emptyList();
        }
        Set<String> codes = effectivePlan.getFeatures().stream()
                .map(Feature::getCode)
                .collect(Collectors.toSet());
        return codes.stream().sorted().toList();
    }

    private BillingPlan resolveOperationalPlan(Condominium condominium) {
        if (subscriptionRepository != null) {
            var subOpt = subscriptionRepository.findByCondominiumId(condominium.getId());
            if (subOpt.isPresent()) {
                CondominiumSubscription sub = subOpt.get();
                if (!isSubscriptionOperational(sub)) {
                    return null;
                }
                return sub.getBillingPlan();
            }
        }
        return condominium.getBillingPlan();
    }

    private boolean isSubscriptionOperational(CondominiumSubscription sub) {
        SubscriptionStatus status = sub.getStatus();
        if (status == null) {
            return false;
        }
        return switch (status) {
            case ACTIVE, PAST_DUE -> true;
            case TRIAL -> sub.getTrialEndsAt() == null || !sub.getTrialEndsAt().isBefore(LocalDateTime.now());
            case SUSPENDED, CANCELLED -> false;
        };
    }

    @CacheEvict(value = "condoFeatures", allEntries = true)
    public void evictCondoFeaturesCache() {
        // Limpa todo o cache ao alterar planos globais ou catálogo de features
    }

    /**
     * Invalida apenas as entradas de cache pertencentes a um condomínio específico,
     * evitando cache stampede global quando um único condomínio muda de plano.
     */
    public void evictCondoFeaturesCache(Long condominiumId) {
        if (condominiumId == null) {
            evictCondoFeaturesCache();
            return;
        }
        if (cacheManager != null) {
            var cache = cacheManager.getCache("condoFeatures");
            if (cache != null) {
                for (FeatureCode code : FeatureCode.values()) {
                    cache.evict(condominiumId + "-" + code.name());
                }
                return;
            }
        }
        evictCondoFeaturesCache();
    }
}