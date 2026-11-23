package com.cefet_rj.projeto_final.billing.controllers;

import com.cefet_rj.projeto_final.billing.dtos.CondominiumSubscriptionDTO;
import com.cefet_rj.projeto_final.billing.dtos.SubscriptionHistoryDTO;
import com.cefet_rj.projeto_final.billing.enums.BillingCycle;
import com.cefet_rj.projeto_final.billing.enums.SubscriptionStatus;
import com.cefet_rj.projeto_final.billing.services.CondominiumSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Assinaturas, ciclo de vida e planos por condomínio")
public class CondominiumSubscriptionController {

    private final CondominiumSubscriptionService subscriptionService;

    @GetMapping("/condominiums/{condominiumId}/subscription")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar assinatura ativa e features liberadas para o condomínio")
    public ResponseEntity<CondominiumSubscriptionDTO> getSubscription(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(subscriptionService.getForCondominium(condominiumId));
    }

    @GetMapping("/condominiums/{condominiumId}/subscription/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Histórico de alterações, upgrades e status da assinatura do condomínio")
    public ResponseEntity<List<SubscriptionHistoryDTO>> getHistory(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(subscriptionService.getHistory(condominiumId));
    }

    @PutMapping("/manager/condominiums/{condominiumId}/subscription/plan")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Alterar o plano de assinatura do condomínio (síndico/admin)")
    public ResponseEntity<CondominiumSubscriptionDTO> changePlanByManager(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(handlePlanChange(condominiumId, body));
    }

    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas as assinaturas dos condomínios")
    public ResponseEntity<List<CondominiumSubscriptionDTO>> listAllForAdmin() {
        return ResponseEntity.ok(subscriptionService.listAllForAdmin());
    }

    @PutMapping("/admin/subscriptions/{condominiumId}/plan")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar plano de assinatura de qualquer condomínio (admin)")
    public ResponseEntity<CondominiumSubscriptionDTO> changePlanByAdmin(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(handlePlanChange(condominiumId, body));
    }

    @PatchMapping("/admin/subscriptions/{condominiumId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar status da assinatura (TRIAL, ACTIVE, PAST_DUE, SUSPENDED, CANCELLED)")
    public ResponseEntity<CondominiumSubscriptionDTO> updateStatus(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        if (body == null || body.get("status") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o status da assinatura.");
        }
        SubscriptionStatus status;
        try {
            status = SubscriptionStatus.valueOf(body.get("status").toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de assinatura inválido.");
        }
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(subscriptionService.updateStatus(condominiumId, status, reason));
    }

    @PostMapping("/admin/subscriptions/{condominiumId}/renew")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Renovar ciclo de assinatura do condomínio")
    public ResponseEntity<CondominiumSubscriptionDTO> renew(
            @PathVariable Long condominiumId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Integer months = body != null && body.get("months") instanceof Number n ? n.intValue() : 1;
        String reason = body != null && body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(subscriptionService.renew(condominiumId, months, reason));
    }

    private CondominiumSubscriptionDTO handlePlanChange(Long condominiumId, Map<String, Object> body) {
        if (body == null || body.get("planId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o planId desejado.");
        }
        Long planId;
        Object rawPlanId = body.get("planId");
        if (rawPlanId instanceof Number n) {
            planId = n.longValue();
        } else {
            try {
                planId = Long.parseLong(rawPlanId.toString().trim());
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planId inválido.");
            }
        }
        BillingCycle cycle = null;
        if (body.get("billingCycle") != null && !body.get("billingCycle").toString().isBlank()) {
            try {
                cycle = BillingCycle.valueOf(body.get("billingCycle").toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ciclo de cobrança inválido (MONTHLY ou ANNUAL).");
            }
        }
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return subscriptionService.changePlan(condominiumId, planId, cycle, reason);
    }
}
