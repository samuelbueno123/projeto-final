package com.cefet_rj.projeto_final.manager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ManagerDashboardDTO(Long condominiumId, String condominiumName, long totalUnits,
        long totalUsers, long totalReservations, List<UnitInfo> units, List<ReservationInfo> reservations) {
    public record UnitInfo(Long id, String identifier, String sector, String paymentStatus, LocalDate dueDate, BigDecimal amount) {}
    public record ReservationInfo(Long id, LocalDateTime startAt, LocalDateTime endAt, String status,
            String commonArea, String residentName, String residentEmail) {}
}
