package com.cefet_rj.projeto_final.reports.dtos;

public record ReservationReportDTO(

        long total,
        long confirmed,
        long cancelled,
        long futureConfirmed

) {
}