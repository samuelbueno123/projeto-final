package com.cefet_rj.projeto_final.reservations.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReservationRequestDTO(

        @NotNull
        @Future
        LocalDateTime startAt,

        @NotNull
        @Future
        LocalDateTime endAt,

        Long reservedById,

        /** Telefone de contato com DDD, somente dígitos (10 ou 11). Opcional. */
        @Pattern(regexp = "^$|^\\d{10,11}$", message = "Phone must contain only digits (DDD + number)")
        String contactPhone,

        @Size(max = 500)
        String notes

) {
    public ReservationRequestDTO(LocalDateTime startAt, LocalDateTime endAt) {
        this(startAt, endAt, null, null, null);
    }

    public ReservationRequestDTO(
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long reservedById
    ) {
        this(startAt, endAt, reservedById, null, null);
    }

    public ReservationRequestDTO(
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long reservedById,
            String contactPhone
    ) {
        this(startAt, endAt, reservedById, contactPhone, null);
    }

    @AssertTrue(message = "Reservation end time must be after start time")
    public boolean isPeriodValid() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return endAt.isAfter(startAt);
    }
}
