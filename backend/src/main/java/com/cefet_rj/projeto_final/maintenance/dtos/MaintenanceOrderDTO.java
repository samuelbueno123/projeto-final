package com.cefet_rj.projeto_final.maintenance.dtos;

import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceOrder;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MaintenanceOrderDTO(
        Long id,
        Long condominiumId,
        String title,
        String description,
        String category,
        String type,
        String priority,
        String status,
        String contractorName,
        String contractorContact,
        LocalDate scheduledDate,
        LocalDateTime completedAt,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        String notes,
        String createdByName,
        LocalDateTime createdAt
) {
    public MaintenanceOrderDTO(MaintenanceOrder entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getType(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getContractorName(),
                entity.getContractorContact(),
                entity.getScheduledDate(),
                entity.getCompletedAt(),
                entity.getEstimatedCost(),
                entity.getActualCost(),
                entity.getNotes(),
                entity.getCreatedBy() != null
                        ? (entity.getCreatedBy().getFirstName() + " " + entity.getCreatedBy().getLastName()).trim()
                        : "Sistema",
                entity.getCreatedAt()
        );
    }

    public record CreateMaintenanceOrderDTO(
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String category,
            String type,
            String priority,
            String contractorName,
            String contractorContact,
            LocalDate scheduledDate,
            BigDecimal estimatedCost,
            String notes
    ) {}

    public record UpdateStatusDTO(
            @NotBlank String status,
            BigDecimal actualCost,
            String notes
    ) {}
}
