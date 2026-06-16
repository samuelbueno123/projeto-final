package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.contracts.BillingPlanInterface;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Setter
@Service
@RequiredArgsConstructor
public class BillingPlanService {

	private BillingPlanInterface billingPlan;

	public ResponseEntity<BillingPlanService> calculateMonthlyFee(BillingPlanInterface billingPlan) {
		billingPlan.calculateMonthlyFee();
		return ResponseEntity.ok(this);
	}

}
