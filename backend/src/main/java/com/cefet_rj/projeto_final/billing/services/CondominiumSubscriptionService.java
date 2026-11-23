package com.cefet_rj.projeto_final.billing.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.dtos.CondominiumSubscriptionDTO;
import com.cefet_rj.projeto_final.billing.dtos.SubscriptionHistoryDTO;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.CondominiumSubscription;
import com.cefet_rj.projeto_final.billing.entities.SubscriptionHistory;
import com.cefet_rj.projeto_final.billing.enums.BillingCycle;
import com.cefet_rj.projeto_final.billing.enums.SubscriptionStatus;
import com.cefet_rj.projeto_final.billing.events.SubscriptionUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.CondominiumSubscriptionRepository;
import com.cefet_rj.projeto_final.billing.repositories.SubscriptionHistoryRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.shared.observability.AuditLogService;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CondominiumSubscriptionService {

    private final CondominiumSubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final CondominiumRepository condominiumRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final CondominiumAccessService condominiumAccessService;
    private final AuthenticatedUserService authenticatedUserService;

    @Autowired(required = false)
    private CondominiumFeatureAccessService featureAccessService;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public CondominiumSubscription ensureSubscriptionExists(Condominium condominium) {
        return subscriptionRepository.findByCondominiumId(condominium.getId())
                .orElseGet(() -> {
                    CondominiumSubscription sub = new CondominiumSubscription();
                    sub.setCondominium(condominium);
                    sub.setBillingPlan(condominium.getBillingPlan());
                    sub.setStatus(Boolean.TRUE.equals(condominium.getIsActive())
                            ? SubscriptionStatus.ACTIVE
                            : SubscriptionStatus.SUSPENDED);
                    sub.setBillingCycle(BillingCycle.MONTHLY);
                    LocalDateTime now = LocalDateTime.now();
                    sub.setStartedAt(now);
                    sub.setCurrentPeriodStart(now);
                    sub.setCurrentPeriodEnd(now.plusMonths(1));
                    sub.setAutoRenew(Boolean.TRUE);
                    sub.setUpdatedAt(now);
                    sub = subscriptionRepository.save(sub);
                    recordHistory(
                            condominium,
                            sub,
                            null,
                            condominium.getBillingPlan(),
                            null,
                            sub.getStatus().name(),
                            "CREATED",
                            "Assinatura inicializada automaticamente",
                            null
                    );
                    return sub;
                });
    }

    @Transactional
    public CondominiumSubscriptionDTO getForCondominium(Long condominiumId) {
        User caller = authenticatedUserService.getAuthenticatedUser();
        if (!caller.hasRole("ROLE_ADMIN")) {
            condominiumAccessService.validateUserBelongsToCondominium(caller.getId(), condominiumId);
        }
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));
        CondominiumSubscription sub = ensureSubscriptionExists(condo);
        return new CondominiumSubscriptionDTO(sub);
    }

    @Transactional(readOnly = true)
    public List<CondominiumSubscriptionDTO> listAllForAdmin() {
        ensureAdminCaller();
        // Garante visibilidade inclusive de condomínios sem registro prévio em tb_condominium_subscription
        List<CondominiumSubscription> existing = subscriptionRepository.findAllWithDetails();
        return existing.stream().map(CondominiumSubscriptionDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionHistoryDTO> getHistory(Long condominiumId) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        if (!condominiumRepository.existsById(condominiumId)) {
            throw new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId);
        }
        return historyRepository.findTop50ByCondominiumIdOrderByCreatedAtDesc(condominiumId).stream()
                .map(SubscriptionHistoryDTO::new)
                .toList();
    }

    @Transactional
    public CondominiumSubscriptionDTO changePlan(
            Long condominiumId,
            Long planId,
            BillingCycle cycle,
            String reason
    ) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        if (planId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o plano desejado.");
        }
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));
        BillingPlan newPlan = billingPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + planId));
        if (!Boolean.TRUE.equals(newPlan.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O plano selecionado não está ativo.");
        }

        CondominiumSubscription sub = ensureSubscriptionExists(condo);
        BillingPlan previousPlan = sub.getBillingPlan();
        String previousStatus = sub.getStatus().name();

        LocalDateTime now = LocalDateTime.now();
        BillingCycle resolvedCycle = cycle != null ? cycle : sub.getBillingCycle();
        sub.setBillingPlan(newPlan);
        sub.setBillingCycle(resolvedCycle);
        sub.setUpdatedAt(now);
        if (sub.getCurrentPeriodEnd() == null || sub.getCurrentPeriodEnd().isBefore(now)) {
            sub.setCurrentPeriodStart(now);
            sub.setCurrentPeriodEnd(resolvedCycle == BillingCycle.ANNUAL ? now.plusYears(1) : now.plusMonths(1));
        }

        condo.setBillingPlan(newPlan);
        condo.setPlanSelected(true);
        condominiumRepository.save(condo);
        sub = subscriptionRepository.save(sub);

        User actor = authenticatedUserService.getAuthenticatedUser();
        String eventType = resolvePlanChangeEventType(previousPlan, newPlan);
        recordHistory(
                condo,
                sub,
                previousPlan,
                newPlan,
                previousStatus,
                sub.getStatus().name(),
                eventType,
                reason != null && !reason.isBlank() ? reason.trim() : "Alteração de plano para " + newPlan.getName(),
                actor
        );

        if (featureAccessService != null) {
            featureAccessService.evictCondoFeaturesCache(condominiumId);
        }
        publishSubscriptionUpdated(condominiumId, newPlan.getName(), sub.getStatus().name(), eventType);
        if (auditLogService != null) {
            auditLogService.logAdminOperation(
                    actor.getEmail(),
                    "SUBSCRIPTION_PLAN_CHANGED",
                    "SUBSCRIPTION",
                    String.valueOf(sub.getId()),
                    condo.getId(),
                    condo.getName(),
                    true,
                    "Plano alterado de " + (previousPlan != null ? previousPlan.getName() : "N/A")
                            + " para " + newPlan.getName() + " (" + resolvedCycle.name() + ")"
            );
        }
        return new CondominiumSubscriptionDTO(sub);
    }

    @Transactional
    public CondominiumSubscriptionDTO updateStatus(
            Long condominiumId,
            SubscriptionStatus newStatus,
            String reason
    ) {
        ensureAdminCaller();
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Novo status da assinatura é obrigatório.");
        }
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));
        CondominiumSubscription sub = ensureSubscriptionExists(condo);

        String previousStatus = sub.getStatus().name();
        LocalDateTime now = LocalDateTime.now();
        sub.setStatus(newStatus);
        sub.setUpdatedAt(now);

        if (newStatus == SubscriptionStatus.TRIAL && sub.getTrialEndsAt() == null) {
            sub.setTrialEndsAt(now.plusDays(14));
        } else if (newStatus == SubscriptionStatus.CANCELLED) {
            sub.setCancelledAt(now);
            sub.setAutoRenew(Boolean.FALSE);
        } else if (newStatus == SubscriptionStatus.ACTIVE) {
            sub.setCancelledAt(null);
            if (sub.getCurrentPeriodEnd() == null || sub.getCurrentPeriodEnd().isBefore(now)) {
                sub.setCurrentPeriodStart(now);
                sub.setCurrentPeriodEnd(sub.getBillingCycle() == BillingCycle.ANNUAL ? now.plusYears(1) : now.plusMonths(1));
            }
        }

        sub = subscriptionRepository.save(sub);

        User actor = authenticatedUserService.getAuthenticatedUser();
        recordHistory(
                condo,
                sub,
                sub.getBillingPlan(),
                sub.getBillingPlan(),
                previousStatus,
                newStatus.name(),
                "STATUS_CHANGED",
                reason != null && !reason.isBlank() ? reason.trim() : "Alteração de status para " + newStatus.name(),
                actor
        );

        if (featureAccessService != null) {
            featureAccessService.evictCondoFeaturesCache(condominiumId);
        }
        publishSubscriptionUpdated(condominiumId,
                sub.getBillingPlan() != null ? sub.getBillingPlan().getName() : null,
                newStatus.name(),
                "STATUS_CHANGED");
        if (auditLogService != null) {
            auditLogService.logAdminOperation(
                    actor.getEmail(),
                    "SUBSCRIPTION_STATUS_CHANGED",
                    "SUBSCRIPTION",
                    String.valueOf(sub.getId()),
                    condo.getId(),
                    condo.getName(),
                    true,
                    "Status da assinatura alterado: " + previousStatus + " -> " + newStatus.name()
                            + (reason != null && !reason.isBlank() ? " | Motivo: " + reason.trim() : "")
            );
        }
        return new CondominiumSubscriptionDTO(sub);
    }

    @Transactional
    public CondominiumSubscriptionDTO renew(Long condominiumId, Integer months, String reason) {
        ensureAdminCaller();
        int periodMonths = months == null || months <= 0 ? 1 : Math.min(months, 36);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));
        CondominiumSubscription sub = ensureSubscriptionExists(condo);

        String previousStatus = sub.getStatus().name();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = sub.getCurrentPeriodEnd() != null && sub.getCurrentPeriodEnd().isAfter(now)
                ? sub.getCurrentPeriodEnd()
                : now;

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCancelledAt(null);
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(base.plusMonths(periodMonths));
        sub.setUpdatedAt(now);
        sub = subscriptionRepository.save(sub);

        User actor = authenticatedUserService.getAuthenticatedUser();
        recordHistory(
                condo,
                sub,
                sub.getBillingPlan(),
                sub.getBillingPlan(),
                previousStatus,
                SubscriptionStatus.ACTIVE.name(),
                "RENEWED",
                reason != null && !reason.isBlank()
                        ? reason.trim()
                        : "Renovação de assinatura por +" + periodMonths + " mês(es)",
                actor
        );

        if (featureAccessService != null) {
            featureAccessService.evictCondoFeaturesCache(condominiumId);
        }
        publishSubscriptionUpdated(condominiumId,
                sub.getBillingPlan() != null ? sub.getBillingPlan().getName() : null,
                SubscriptionStatus.ACTIVE.name(),
                "RENEWED");
        if (auditLogService != null) {
            auditLogService.logAdminOperation(
                    actor.getEmail(),
                    "SUBSCRIPTION_RENEWED",
                    "SUBSCRIPTION",
                    String.valueOf(sub.getId()),
                    condo.getId(),
                    condo.getName(),
                    true,
                    "Assinatura renovada por " + periodMonths + " mês(es) até " + sub.getCurrentPeriodEnd()
            );
        }
        return new CondominiumSubscriptionDTO(sub);
    }

    private String resolvePlanChangeEventType(BillingPlan previousPlan, BillingPlan newPlan) {
        if (previousPlan == null || previousPlan.getMonthlyFee() == null || newPlan.getMonthlyFee() == null) {
            return "PLAN_CHANGED";
        }
        int cmp = newPlan.getMonthlyFee().compareTo(previousPlan.getMonthlyFee());
        if (cmp > 0) return "UPGRADED";
        if (cmp < 0) return "DOWNGRADED";
        return "PLAN_CHANGED";
    }

    private void recordHistory(
            Condominium condominium,
            CondominiumSubscription subscription,
            BillingPlan previousPlan,
            BillingPlan newPlan,
            String previousStatus,
            String newStatus,
            String eventType,
            String reason,
            User actor
    ) {
        SubscriptionHistory h = new SubscriptionHistory();
        h.setCondominium(condominium);
        h.setSubscription(subscription);
        h.setPreviousPlan(previousPlan);
        h.setNewPlan(newPlan);
        h.setPreviousStatus(previousStatus);
        h.setNewStatus(newStatus);
        h.setEventType(eventType);
        h.setReason(reason);
        h.setChangedBy(actor);
        h.setCreatedAt(LocalDateTime.now());
        historyRepository.save(h);
    }

    private void publishSubscriptionUpdated(Long condominiumId, String planName, String status, String eventType) {
        if (eventPublisher != null && condominiumId != null) {
            eventPublisher.publishEvent(new SubscriptionUpdatedEvent(condominiumId, planName, status, eventType));
        }
    }

    private void ensureAdminCaller() {
        User caller = authenticatedUserService.getAuthenticatedUser();
        if (!caller.hasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores.");
        }
    }
}
