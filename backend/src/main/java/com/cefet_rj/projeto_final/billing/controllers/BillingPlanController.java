package com.cefet_rj.projeto_final.billing.controllers;

import com.cefet_rj.projeto_final.billing.enums.CondominiumBillingPlan;
import com.cefet_rj.projeto_final.billing.services.BillingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/billing-plans")
@RequiredArgsConstructor
public class BillingPlanController {

	private final BillingPlanService billingPlanService;

	@GetMapping("/{plan}/monthly-fee")
	public ResponseEntity<BigDecimal> calculate(@PathVariable String plan) {
		CondominiumBillingPlan billingPlan = CondominiumBillingPlan.fromString(plan);
		BigDecimal fee = billingPlanService.calculateMonthlyFee(billingPlan);
		return ResponseEntity.ok(fee);
	}
}
