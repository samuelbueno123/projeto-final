package com.cefet_rj.projeto_final.reports.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardReportDTO(

        Long condominiumId,
        String condominiumName,
        LocalDate startDate,
        LocalDate endDate,

        PackageReportDTO packages,
        long activeAnnouncements,
        ReservationReportDTO reservations,

        VisitorReportDTO visitors,
        TicketReportDTO tickets,
        MaintenanceReportDTO maintenance,
        List<CommonAreaUtilizationDTO> commonAreaUtilization,
        FinancialReportDTO financial,
        CommunitySummaryDTO community

) {

    public DashboardReportDTO(
            Long condominiumId,
            PackageReportDTO packages,
            long activeAnnouncements,
            ReservationReportDTO reservations
    ) {
        this(
                condominiumId,
                null,
                null,
                null,
                packages,
                activeAnnouncements,
                reservations,
                new VisitorReportDTO(0, 0, 0, 0, 0),
                new TicketReportDTO(0, 0, 0, 0),
                new MaintenanceReportDTO(0, 0, 0, 0, 0, 0, BigDecimal.ZERO),
                List.of(),
                new FinancialReportDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new CommunitySummaryDTO(0, 0, 0, 0, 0, 0)
        );
    }

    public record VisitorReportDTO(
            long total,
            long authorized,
            long checkedIn,
            long checkedOut,
            long denied
    ) {}

    public record TicketReportDTO(
            long total,
            long open,
            long inProgress,
            long resolved
    ) {}

    public record MaintenanceReportDTO(
            long totalOrders,
            long openOrders,
            long completedOrders,
            long preventiveOrders,
            long overdueEquipment,
            long dueSoonEquipment,
            BigDecimal totalActualCost
    ) {}

    public record CommonAreaUtilizationDTO(
            Long commonAreaId,
            String commonAreaName,
            long confirmedBookings,
            long pendingBookings,
            long waitlistedBookings,
            long cancelledBookings
    ) {}

    public record FinancialReportDTO(
            BigDecimal totalInvoiced,
            BigDecimal totalCollected,
            BigDecimal totalPending,
            long overdueUnits
    ) {}

    public record CommunitySummaryDTO(
            long totalVehicles,
            long pendingVehicles,
            long totalParkingSpots,
            long occupiedParkingSpots,
            long totalPets,
            long openLostFoundItems
    ) {}
}
