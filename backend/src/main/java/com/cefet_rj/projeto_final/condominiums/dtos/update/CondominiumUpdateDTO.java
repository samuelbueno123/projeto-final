package com.cefet_rj.projeto_final.condominiums.dtos.update;

import jakarta.validation.constraints.NotBlank;

public record CondominiumUpdateDTO(

        @NotBlank
        String name,

        @NotBlank
        String address,

        @NotBlank
        String billingPlan,

        Boolean isActive

) {
}