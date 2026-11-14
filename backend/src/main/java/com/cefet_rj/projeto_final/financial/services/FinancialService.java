package com.cefet_rj.projeto_final.financial.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.CreateExpenseDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.FinancialSummaryDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.BudgetForecastDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.CategoryBudgetComparisonDTO;
import com.cefet_rj.projeto_final.financial.dtos.ExpenseDTO.BudgetComparisonSummaryDTO;
import com.cefet_rj.projeto_final.financial.entities.BudgetForecast;
import com.cefet_rj.projeto_final.financial.entities.CondominiumExpense;
import com.cefet_rj.projeto_final.financial.repositories.BudgetForecastRepository;
import com.cefet_rj.projeto_final.financial.repositories.CondominiumExpenseRepository;
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinancialService {

    private final CondominiumExpenseRepository expenseRepository;
    private final BudgetForecastRepository budgetRepository;
    private final CondominiumRepository condominiumRepository;
    private final UnitRepository unitRepository;
    private final UnitPaymentRepository paymentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final RealtimeEmitterService realtimeEmitterService;

    @Transactional(readOnly = true)
    public List<ExpenseDTO> listExpenses(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        return expenseRepository.findByCondominiumIdOrderByDueDateDesc(condominiumId).stream()
                .map(ExpenseDTO::new)
                .toList();
    }

    @Transactional
    public ExpenseDTO createExpense(Long condominiumId, CreateExpenseDTO dto) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        User caller = authenticatedUserService.getAuthenticatedUser();

        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor da despesa deve ser positivo.");
        }

        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        CondominiumExpense expense = new CondominiumExpense();
        expense.setCondominium(condo);
        expense.setCategory(dto.category().trim().toUpperCase());
        expense.setDescription(dto.description().trim());
        expense.setAmount(dto.amount());
        expense.setDueDate(dto.dueDate());
        expense.setSupplier(dto.supplier() != null ? dto.supplier().trim() : null);
        expense.setDocumentNumber(dto.documentNumber() != null ? dto.documentNumber().trim() : null);
        expense.setReceiptUrl(dto.receiptUrl() != null ? dto.receiptUrl().trim() : null);
        expense.setStatus("PENDING");
        expense.setCreatedBy(caller);
        expense.setCreatedAt(LocalDateTime.now());

        expense = expenseRepository.save(expense);
        ExpenseDTO result = new ExpenseDTO(expense);

        realtimeEmitterService.sendToCondominium(condominiumId,
                RealtimeEventDTO.of("INVOICE_UPDATED", condominiumId, result));

        return result;
    }

    @Transactional
    public ExpenseDTO payExpense(Long expenseId) {
        CondominiumExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada: " + expenseId));

        condominiumAccessService.validateManagerAccess(expense.getCondominium().getId());

        String currentStatus = expense.getStatus() != null ? expense.getStatus().toUpperCase() : "PENDING";
        if ("PAID".equals(currentStatus)) {
            return new ExpenseDTO(expense);
        }
        if ("CANCELLED".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível liquidar uma despesa que foi cancelada.");
        }

        expense.setStatus("PAID");
        expense.setPaidAt(LocalDateTime.now());
        expense = expenseRepository.save(expense);

        ExpenseDTO result = new ExpenseDTO(expense);
        realtimeEmitterService.sendToCondominium(expense.getCondominium().getId(),
                RealtimeEventDTO.of("INVOICE_UPDATED", expense.getCondominium().getId(), result));

        return result;
    }

    @Transactional
    public ExpenseDTO cancelExpense(Long expenseId) {
        CondominiumExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada: " + expenseId));

        condominiumAccessService.validateManagerAccess(expense.getCondominium().getId());

        String currentStatus = expense.getStatus() != null ? expense.getStatus().toUpperCase() : "PENDING";
        if ("PAID".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível cancelar uma despesa que já foi paga.");
        }
        if ("CANCELLED".equals(currentStatus)) {
            return new ExpenseDTO(expense);
        }

        expense.setStatus("CANCELLED");
        expense = expenseRepository.save(expense);

        ExpenseDTO result = new ExpenseDTO(expense);
        realtimeEmitterService.sendToCondominium(expense.getCondominium().getId(),
                RealtimeEventDTO.of("INVOICE_UPDATED", expense.getCondominium().getId(), result));

        return result;
    }

    @Transactional(readOnly = true)
    public FinancialSummaryDTO getFinancialSummary(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);

        List<Unit> units = unitRepository.findAllForCondominium(condominiumId);
        List<Long> unitIds = units.stream().map(Unit::getId).toList();
        List<UnitPayment> payments = unitIds.isEmpty() ? List.of() : paymentRepository.findAllByUnitIdIn(unitIds);

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;
        long unitsPaid = 0;
        long unitsPending = 0;

        for (UnitPayment p : payments) {
            BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            totalInvoiced = totalInvoiced.add(amt);
            if (p.getStatus() == UnitPaymentStatus.PAID) {
                totalCollected = totalCollected.add(amt);
                unitsPaid++;
            } else {
                totalPending = totalPending.add(amt);
                unitsPending++;
            }
        }

        List<CondominiumExpense> allExpenses = expenseRepository.findByCondominiumIdOrderByDueDateDesc(condominiumId);
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (CondominiumExpense exp : allExpenses) {
            if ("PAID".equalsIgnoreCase(exp.getStatus())) {
                totalExpenses = totalExpenses.add(exp.getAmount());
            }
        }

        BigDecimal netBalance = totalCollected.subtract(totalExpenses);

        List<ExpenseDTO> recentExpenses = allExpenses.stream()
                .limit(10)
                .map(ExpenseDTO::new)
                .toList();

        return new FinancialSummaryDTO(
                totalInvoiced,
                totalCollected,
                totalPending,
                totalExpenses,
                netBalance,
                unitsPaid,
                unitsPending,
                recentExpenses
        );
    }

    // =========================================================================
    // PREVISÃO ORÇAMENTÁRIA vs REALIZADO (Budget Forecasting & Category Tracking)
    // =========================================================================

    @Transactional(readOnly = true)
    public BudgetComparisonSummaryDTO getBudgetComparison(Long condominiumId, Integer year, Integer month) {
        condominiumAccessService.validateAccess(condominiumId);
        int targetYear = (year != null && year > 2000) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();

        List<BudgetForecast> forecasts = budgetRepository.findByCondominiumIdAndYearAndMonth(condominiumId, targetYear, targetMonth);
        Map<String, BigDecimal> budgetMap = new LinkedHashMap<>();
        for (BudgetForecast b : forecasts) {
            budgetMap.put(b.getCategory().toUpperCase(Locale.ROOT), b.getBudgetedAmount());
        }

        // Standard categories if not present
        List<String> defaultCategories = List.of("PESSOAL", "MANUTENCAO", "LIMPEZA", "CONCESSIONARIAS", "ADMINISTRATIVO", "OUTROS");
        for (String cat : defaultCategories) {
            budgetMap.putIfAbsent(cat, BigDecimal.ZERO);
        }

        List<CondominiumExpense> expenses = expenseRepository.findByCondominiumIdOrderByDueDateDesc(condominiumId);
        Map<String, BigDecimal> actualMap = new LinkedHashMap<>();
        for (CondominiumExpense e : expenses) {
            if ("CANCELLED".equalsIgnoreCase(e.getStatus())) continue;
            if (e.getDueDate() != null && e.getDueDate().getYear() == targetYear && e.getDueDate().getMonthValue() == targetMonth) {
                String cat = (e.getCategory() != null && !e.getCategory().isBlank())
                        ? e.getCategory().trim().toUpperCase(Locale.ROOT)
                        : "OUTROS";
                BigDecimal current = actualMap.getOrDefault(cat, BigDecimal.ZERO);
                actualMap.put(cat, current.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO));
            }
        }

        Set<String> allCategories = new LinkedHashSet<>(budgetMap.keySet());
        allCategories.addAll(actualMap.keySet());

        List<CategoryBudgetComparisonDTO> categoryList = new ArrayList<>();
        BigDecimal totalBudgeted = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;

        for (String cat : allCategories) {
            BigDecimal budgeted = budgetMap.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal actual = actualMap.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal variance = budgeted.subtract(actual);

            BigDecimal percentageUsed = BigDecimal.ZERO;
            if (budgeted.compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = actual.multiply(BigDecimal.valueOf(100)).divide(budgeted, 1, RoundingMode.HALF_UP);
            } else if (actual.compareTo(BigDecimal.ZERO) > 0) {
                percentageUsed = BigDecimal.valueOf(100.0);
            }

            String status = "ON_TRACK";
            if (actual.compareTo(budgeted) > 0) {
                status = "OVER_BUDGET";
            } else if (percentageUsed.compareTo(BigDecimal.valueOf(80)) < 0) {
                status = "UNDER_BUDGET";
            }

            categoryList.add(new CategoryBudgetComparisonDTO(cat, budgeted, actual, variance, percentageUsed, status));
            totalBudgeted = totalBudgeted.add(budgeted);
            totalActual = totalActual.add(actual);
        }

        BigDecimal totalVariance = totalBudgeted.subtract(totalActual);
        BigDecimal totalPercentageUsed = BigDecimal.ZERO;
        if (totalBudgeted.compareTo(BigDecimal.ZERO) > 0) {
            totalPercentageUsed = totalActual.multiply(BigDecimal.valueOf(100)).divide(totalBudgeted, 1, RoundingMode.HALF_UP);
        } else if (totalActual.compareTo(BigDecimal.ZERO) > 0) {
            totalPercentageUsed = BigDecimal.valueOf(100.0);
        }

        String overallStatus = "ON_TRACK";
        if (totalActual.compareTo(totalBudgeted) > 0) {
            overallStatus = "OVER_BUDGET";
        } else if (totalPercentageUsed.compareTo(BigDecimal.valueOf(85)) < 0) {
            overallStatus = "UNDER_BUDGET";
        }

        return new BudgetComparisonSummaryDTO(
                targetYear,
                targetMonth,
                totalBudgeted,
                totalActual,
                totalVariance,
                totalPercentageUsed,
                overallStatus,
                categoryList
        );
    }

    @Transactional
    public BudgetForecastDTO saveBudgetForecast(Long condominiumId, Map<String, Object> body) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        int year = LocalDate.now().getYear();
        if (body.get("year") instanceof Number n) year = n.intValue();
        int month = LocalDate.now().getMonthValue();
        if (body.get("month") instanceof Number n) month = n.intValue();
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês deve estar entre 1 e 12.");
        }
        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano inválido (esperado entre 2000 e 2100).");
        }

        String category = body.get("category") != null ? body.get("category").toString().trim().toUpperCase(Locale.ROOT) : "OUTROS";
        BigDecimal amount = BigDecimal.ZERO;
        if (body.get("budgetedAmount") instanceof Number n) amount = BigDecimal.valueOf(n.doubleValue());
        else if (body.get("budgetedAmount") != null) {
            try { amount = new BigDecimal(body.get("budgetedAmount").toString().trim()); } catch (Exception ignored) {}
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor orçado não pode ser negativo.");
        }

        String notes = body.get("notes") != null ? body.get("notes").toString().trim() : null;
        User user = authenticatedUserService.getAuthenticatedUser();

        BudgetForecast forecast = budgetRepository.findByCondominiumIdAndYearAndMonthAndCategory(condominiumId, year, month, category)
                .orElseGet(BudgetForecast::new);

        forecast.setCondominium(condo);
        forecast.setYear(year);
        forecast.setMonth(month);
        forecast.setCategory(category);
        forecast.setBudgetedAmount(amount);
        forecast.setNotes(notes);
        forecast.setCreatedBy(user);
        if (forecast.getId() == null) forecast.setCreatedAt(LocalDateTime.now());

        forecast = budgetRepository.save(forecast);
        return new BudgetForecastDTO(
                forecast.getId(),
                condominiumId,
                forecast.getYear(),
                forecast.getMonth(),
                forecast.getCategory(),
                forecast.getBudgetedAmount(),
                forecast.getNotes(),
                forecast.getCreatedAt()
        );
    }
}
