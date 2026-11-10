package com.cefet_rj.projeto_final.financial.controllers;

import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.CreateExpenseDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.FinancialSummaryDTO;
import com.cefet_rj.projeto_final.financial.services.FinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Financial Management", description = "Gestão financeira de despesas, prestação de contas e transparência condominial")
public class FinancialController {

    private final FinancialService financialService;

    @GetMapping("/condominiums/{condominiumId}/financial/summary")
    @Operation(summary = "Obtém balancete e resumo financeiro do condomínio")
    public ResponseEntity<FinancialSummaryDTO> getSummary(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(financialService.getFinancialSummary(condominiumId));
    }

    @GetMapping("/condominiums/{condominiumId}/financial/expenses")
    @Operation(summary = "Lista todas as despesas lançadas no condomínio")
    public ResponseEntity<List<ExpenseDTO>> listExpenses(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(financialService.listExpenses(condominiumId));
    }

    @PostMapping("/condominiums/{condominiumId}/financial/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lança uma nova despesa operacional do condomínio")
    public ResponseEntity<ExpenseDTO> createExpense(
            @PathVariable Long condominiumId,
            @Valid @RequestBody CreateExpenseDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financialService.createExpense(condominiumId, dto));
    }

    @PatchMapping("/financial/expenses/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Liquida e dá baixa em uma despesa do condomínio")
    public ResponseEntity<ExpenseDTO> payExpense(@PathVariable Long id) {
        return ResponseEntity.ok(financialService.payExpense(id));
    }

    @PatchMapping("/financial/expenses/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cancela uma despesa do condomínio")
    public ResponseEntity<ExpenseDTO> cancelExpense(@PathVariable Long id) {
        return ResponseEntity.ok(financialService.cancelExpense(id));
    }

    @GetMapping("/condominiums/{condominiumId}/financial/budget-comparison")
    @Operation(summary = "Compara despesas orçadas vs realizadas por categoria no período")
    public ResponseEntity<ExpenseDTO.BudgetComparisonSummaryDTO> getBudgetComparison(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(financialService.getBudgetComparison(condominiumId, year, month));
    }

    @PostMapping("/condominiums/{condominiumId}/financial/budgets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Define ou atualiza meta orçamentária por categoria para um mês/ano")
    public ResponseEntity<ExpenseDTO.BudgetForecastDTO> saveBudgetForecast(
            @PathVariable Long condominiumId,
            @RequestBody java.util.Map<String, Object> body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(financialService.saveBudgetForecast(condominiumId, body));
    }
}
