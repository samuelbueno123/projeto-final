package com.cefet_rj.projeto_final.maintenance.dtos;

import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceOrder;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MaintenanceOrderDTO(
        Long id,
        Long condominiumId,
        Long equipmentId,
        String equipmentName,
        Long supplierId,
        String supplierCompanyName,
        String title,
        String description,
        String category,
        String type,
        String priority,
        String status,
        String contractorName,
        String contractorContact,
        String responsibleName,
        Integer recurrenceDays,
        LocalDate scheduledDate,
        LocalDateTime completedAt,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        Integer supplierRating,
        String supplierFeedback,
        String notes,
        String createdByName,
        LocalDateTime createdAt
) {
    public MaintenanceOrderDTO(MaintenanceOrder entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getEquipment() != null ? entity.getEquipment().getId() : null,
                entity.getEquipment() != null ? entity.getEquipment().getName() : null,
                entity.getSupplier() != null ? entity.getSupplier().getId() : null,
                entity.getSupplier() != null ? entity.getSupplier().getCompanyName() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getType(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getContractorName(),
                entity.getContractorContact(),
                entity.getResponsibleName(),
                entity.getRecurrenceDays(),
                entity.getScheduledDate(),
                entity.getCompletedAt(),
                entity.getEstimatedCost(),
                entity.getActualCost(),
                entity.getSupplierRating(),
                entity.getSupplierFeedback(),
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
            Long equipmentId,
            Long supplierId,
            String contractorName,
            String contractorContact,
            String responsibleName,
            Integer recurrenceDays,
            LocalDate scheduledDate,
            BigDecimal estimatedCost,
            String notes
    ) {}

    public record UpdateStatusDTO(
            @NotBlank String status,
            BigDecimal actualCost,
            Integer supplierRating,
            String supplierFeedback,
            String notes
    ) {}
}
