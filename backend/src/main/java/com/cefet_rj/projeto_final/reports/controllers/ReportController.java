package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.reports.dtos.DashboardReportDTO;
import com.cefet_rj.projeto_final.reports.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Reports",
        description = "Relatórios, indicadores operacionais e exportação (PDF e CSV/Excel)"
)
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/condominiums/{condominiumId}/reports/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obter indicadores gerenciais e operacionais do condomínio (filtrável por período)")
    public ResponseEntity<DashboardReportDTO> getDashboard(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                reportService.getDashboard(condominiumId, startDate, endDate)
        );
    }

    @GetMapping("/condominiums/{condominiumId}/reports/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Exportar relatório gerencial em formato CSV compatível com Excel")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] csvBytes = reportService.exportCsv(condominiumId, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-condominio-" + condominiumId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    @GetMapping("/condominiums/{condominiumId}/reports/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Exportar relatório gerencial em formato PDF")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdfBytes = reportService.exportPdf(condominiumId, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-condominio-" + condominiumId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
