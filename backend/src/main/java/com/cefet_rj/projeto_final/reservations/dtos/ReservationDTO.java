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
        String counterProposalNotes,
        LocalDateTime proposedStartAt,
        LocalDateTime proposedEndAt,
        Long reviewedById,
        String reviewedByName,
        LocalDateTime reviewedAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt

) {

    public ReservationDTO(
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
        this(id, commonAreaId, commonAreaName, condominiumId, condominiumName,
                reservedById, reservedByName, reservedByEmail, contactPhone, notes, rejectionReason,
                null, null, null, null, null, null, startAt, endAt, status, createdAt, cancelledAt);
    }

    public ReservationDTO(Reservation entity) {
        this(
                entity.getId(),
                entity.getCommonArea().getId(),
                entity.getCommonArea().getName(),
                entity.getCommonArea().getCondominium().getId(),
                entity.getCommonArea().getCondominium().getName(),
                entity.getReservedBy().getId(),
                (entity.getReservedBy().getFirstName() + " " + entity.getReservedBy().getLastName()).trim(),
                entity.getReservedBy().getEmail(),
                entity.getContactPhone(),
                entity.getNotes(),
                entity.getRejectionReason(),
                entity.getCounterProposalNotes(),
                entity.getProposedStartAt(),
                entity.getProposedEndAt(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null,
                entity.getReviewedBy() != null
                        ? (entity.getReviewedBy().getFirstName() + " " + entity.getReviewedBy().getLastName()).trim()
                        : null,
                entity.getReviewedAt(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCancelledAt()
        );
    }

    public record CounterProposalDTO(
            LocalDateTime proposedStartAt,
            LocalDateTime proposedEndAt,
            String notes
    ) {}

    public record ModifyAndConfirmDTO(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String notes
    ) {}
}
