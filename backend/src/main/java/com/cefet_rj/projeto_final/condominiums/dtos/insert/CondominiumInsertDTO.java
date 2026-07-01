package com.cefet_rj.projeto_final.condominiums.dtos.insert;

import jakarta.validation.constraints.NotBlank;

public record CondominiumInsertDTO(
        @NotBlank String name,
        @NotBlank String address,
        @NotBlank String billingPlan,
        Boolean isActive
) {}