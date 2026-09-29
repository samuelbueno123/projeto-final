package com.cefet_rj.projeto_final.reservations.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequestDTO(

        @NotNull
        @Future
        LocalDateTime startAt,

        @NotNull
        @Future
        LocalDateTime endAt

) {

    @AssertTrue(message = "Reservation end time must be after start time")
    public boolean isPeriodValid() {

        if (startAt == null || endAt == null) {
            return true;
        }

        return endAt.isAfter(startAt);
    }
}