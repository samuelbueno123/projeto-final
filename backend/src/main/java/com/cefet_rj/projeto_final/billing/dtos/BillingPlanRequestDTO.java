package com.cefet_rj.projeto_final.billing.dtos;

import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record BillingPlanRequestDTO(

        @NotBlank
        String name,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal monthlyFee,

        @NotNull
        Set<FeatureCode> features

) {
}