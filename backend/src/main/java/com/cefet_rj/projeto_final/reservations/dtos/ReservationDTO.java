package com.cefet_rj.projeto_final.reservations.dtos;

import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationDTO(

        Long id,
        Long commonAreaId,
        String commonAreaName,
        Long condominiumId,
        String condominiumName,
        Long reservedById,
        String reservedByName,
        String reservedByEmail,
        String contactPhone,
        String notes,
        String rejectionReason,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt

) {

    public ReservationDTO(Reservation entity) {
        this(
                entity.getId(),
                entity.getCommonArea().getId(),
                entity.getCommonArea().getName(),
                entity.getCommonArea()
                        .getCondominium()
                        .getId(),
                entity.getCommonArea()
                        .getCondominium()
                        .getName(),
                entity.getReservedBy().getId(),
                (entity.getReservedBy().getFirstName() + " "
                        + entity.getReservedBy().getLastName()).trim(),
                entity.getReservedBy().getEmail(),
                entity.getContactPhone(),
                entity.getNotes(),
                entity.getRejectionReason(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCancelledAt()
        );
    }
}
