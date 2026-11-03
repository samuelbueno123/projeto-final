package com.cefet_rj.projeto_final.financial.dtos;

import com.cefet_rj.projeto_final.financial.entities.CondominiumExpense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseDTO(
        Long id,
        Long condominiumId,
        String category,
        String description,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        String supplier,
        String documentNumber,
        String receiptUrl,
        String status,
        String createdByName,
        LocalDateTime createdAt
) {
    public ExpenseDTO(CondominiumExpense entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getDueDate(),
                entity.getPaidAt(),
                entity.getSupplier(),
                entity.getDocumentNumber(),
                entity.getReceiptUrl(),
                entity.getStatus(),
                entity.getCreatedBy() != null
                        ? (entity.getCreatedBy().getFirstName() + " " + entity.getCreatedBy().getLastName()).trim()
                        : "Sistema",
                entity.getCreatedAt()
        );
    }

    public record CreateExpenseDTO(
            @NotBlank String category,
            @NotBlank String description,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate dueDate,
            String supplier,
            String documentNumber,
            String receiptUrl
    ) {}

    public record FinancialSummaryDTO(
            BigDecimal totalInvoiced,
            BigDecimal totalCollected,
            BigDecimal totalPending,
            BigDecimal totalExpenses,
            BigDecimal netBalance,
            long unitsPaid,
            long unitsPending,
            List<ExpenseDTO> recentExpenses
    ) {}
}
