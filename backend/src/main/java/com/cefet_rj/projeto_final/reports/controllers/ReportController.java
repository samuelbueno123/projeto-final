package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.reports.dtos.DashboardReportDTO;
import com.cefet_rj.projeto_final.reports.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Reports",
        description = "Relatórios e dashboards administrativos"
)
public class ReportController {

    private final ReportService reportService;

    @GetMapping(
            "/condominiums/{condominiumId}/reports/dashboard"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER')"
    )
    public ResponseEntity<DashboardReportDTO>
    getDashboard(
            @PathVariable Long condominiumId
    ) {

        return ResponseEntity.ok(
                reportService.getDashboard(
                        condominiumId
                )
        );
    }
}