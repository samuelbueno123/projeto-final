package com.cefet_rj.projeto_final.manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ManagerDashboardDTO(
        Long condominiumId,
        String condominiumName,
        long totalUnits,
        long totalUsers,
        long totalReservations,
        List<UnitInfo> units,
        List<ReservationInfo> reservations
) {
    public record UnitInfo(
            Long id,
            String identifier,
            String sector,
            String paymentStatus,
            LocalDate dueDate,
            BigDecimal amount,
            String paymentNote,
            LocalDateTime requestedAt,
            String invoiceTitle,
            String digitableLine,
            LocalDateTime paidAt
    ) {
        public UnitInfo(
                Long id,
                String identifier,
                String sector,
                String paymentStatus,
                LocalDate dueDate,
                BigDecimal amount,
                String paymentNote,
                LocalDateTime requestedAt
        ) {
            this(id, identifier, sector, paymentStatus, dueDate, amount, paymentNote, requestedAt, null, null, null);
        }
    }

    public record ReservationInfo(
            Long id,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String status,
            Long commonAreaId,
            String commonArea,
            String residentName,
            String residentEmail,
            String contactPhone,
            String notes,
            String rejectionReason,
            String counterProposalNotes,
            LocalDateTime proposedStartAt,
            LocalDateTime proposedEndAt
    ) {
        public ReservationInfo(
                Long id,
                LocalDateTime startAt,
                LocalDateTime endAt,
                String status,
                Long commonAreaId,
                String commonArea,
                String residentName,
                String residentEmail,
                String contactPhone,
                String notes,
                String rejectionReason
        ) {
            this(id, startAt, endAt, status, commonAreaId, commonArea, residentName, residentEmail, contactPhone, notes, rejectionReason, null, null, null);
        }
    }
}
