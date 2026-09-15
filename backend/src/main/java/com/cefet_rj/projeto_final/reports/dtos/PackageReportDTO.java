package com.cefet_rj.projeto_final.reports.dtos;

public record PackageReportDTO(

        long total,

        long expected,
        long received,
        long notified,
        long pickedUp,
        long cancelled,
        long returned

) {
}