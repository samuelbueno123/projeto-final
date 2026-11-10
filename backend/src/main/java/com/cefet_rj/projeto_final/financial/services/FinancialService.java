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
import com.cefet_rj.projeto_final.financial.entities.CondominiumExpense;
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
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialService {

    private final CondominiumExpenseRepository expenseRepository;
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
}
