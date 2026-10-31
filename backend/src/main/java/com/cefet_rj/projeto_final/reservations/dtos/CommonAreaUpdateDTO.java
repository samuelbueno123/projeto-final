package com.cefet_rj.projeto_final.reservations.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommonAreaUpdateDTO(

        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        @Min(1)
        Integer capacity,

        Boolean requiresApproval,

        @Size(max = 1000)
        String rules,

        @Min(0)
        @Max(23)
        Integer openingHour,

        @Min(1)
        @Max(24)
        Integer closingHour

) {
    public CommonAreaUpdateDTO(String name, String description, Integer capacity) {
        this(name, description, capacity, null, null, null, null);
    }
}
