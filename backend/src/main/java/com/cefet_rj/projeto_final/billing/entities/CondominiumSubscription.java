package com.cefet_rj.projeto_final.billing.entities;

import com.cefet_rj.projeto_final.billing.enums.BillingCycle;
import com.cefet_rj.projeto_final.billing.enums.SubscriptionStatus;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tb_condominium_subscription")
public class CondominiumSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "condominium_id", nullable = false, unique = true)
    private Condominium condominium;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_plan_id", nullable = false)
    private BillingPlan billingPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart = LocalDateTime.now();

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = Boolean.TRUE;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
