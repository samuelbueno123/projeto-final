package com.cefet_rj.projeto_final.billing.dtos;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public record BillingPlanDTO(

        Long id,
        String name,
        BigDecimal monthlyFee,
        Boolean active,
        Set<String> features

) {

    public BillingPlanDTO(BillingPlan entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getMonthlyFee(),
                entity.getActive(),
                entity.getFeatures()
                        .stream()
                        .map(feature -> feature.getCode())
                        .collect(Collectors.toSet())
        );
    }
}