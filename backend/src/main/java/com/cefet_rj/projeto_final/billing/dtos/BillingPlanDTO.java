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
        Set<String> features,
        String code,
        String description,
        Integer maxUnits,
        Integer displayOrder

) {

    public BillingPlanDTO(
            Long id,
            String name,
            BigDecimal monthlyFee,
            Boolean active,
            Set<String> features
    ) {
        this(id, name, monthlyFee, active, features, null, null, null, 0);
    }

    public BillingPlanDTO(BillingPlan entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getMonthlyFee(),
                entity.getActive(),
                entity.getFeatures()
                        .stream()
                        .map(feature -> feature.getCode())
                        .collect(Collectors.toSet()),
                entity.getCode(),
                entity.getDescription(),
                entity.getMaxUnits(),
                entity.getDisplayOrder()
        );
    }
}