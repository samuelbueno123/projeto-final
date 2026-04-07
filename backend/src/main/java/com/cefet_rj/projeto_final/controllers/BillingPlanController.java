package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.contracts.BASIC;
import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;
import com.cefet_rj.projeto_final.contracts.PREMIUM;
import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.services.BillingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billingplans")
@RequiredArgsConstructor
public class BillingPlanController {

	private final BillingPlanService billingPlanService;

	public ResponseEntity<BillingPlanService> calculate(CondominiumDTO condominiumDTO) {

		BillingPlanInterface billingPlan = switch (condominiumDTO.billingPlan().getPlanName()) {
			case "BASIC" -> new BASIC();
			case "PREMIUM" -> new PREMIUM();
			default -> throw new IllegalArgumentException("Invalid billing plan: " + condominiumDTO.billingPlan().getPlanName());
		};

		return billingPlanService.calculateMonthlyFee(billingPlan);
	}

}
