package com.cefet_rj.projeto_final.reports.dtos;

public record DashboardReportDTO(

        Long condominiumId,

        PackageReportDTO packages,

        long activeAnnouncements,

        ReservationReportDTO reservations

) {
}