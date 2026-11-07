package com.cefet_rj.projeto_final.reservations.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommonAreaRequestDTO(

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
        Integer closingHour,

        String imageUrl,

        @Min(0)
        Integer minAdvanceHours,

        @Min(1)
        Integer maxAdvanceDays,

        @Min(1)
        Integer maxDurationHours,

        @Min(1)
        Integer maxWeeklyBookingsPerUser,

        Boolean allowWaitlist

) {
    public CommonAreaRequestDTO(String name, String description, Integer capacity) {
        this(name, description, capacity, true, null, 8, 22, null, 1, 60, 8, 3, true);
    }

    public CommonAreaRequestDTO(
            String name,
            String description,
            Integer capacity,
            Boolean requiresApproval,
            String rules,
            Integer openingHour,
            Integer closingHour,
            String imageUrl
    ) {
        this(name, description, capacity, requiresApproval, rules, openingHour, closingHour, imageUrl, 1, 60, 8, 3, true);
    }
}
