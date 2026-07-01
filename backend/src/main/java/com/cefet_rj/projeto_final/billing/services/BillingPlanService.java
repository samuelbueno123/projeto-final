package com.cefet_rj.projeto_final.billing.services;

import com.cefet_rj.projeto_final.billing.contracts.BillingPlanInterface;
import com.cefet_rj.projeto_final.billing.factories.BillingPlanFactory;
import com.cefet_rj.projeto_final.billing.enums.CondominiumBillingPlan;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BillingPlanService {

	public BigDecimal calculateMonthlyFee(CondominiumBillingPlan plan) {
		BillingPlanInterface billingPlan = BillingPlanFactory.create(plan);
		return billingPlan.calculateMonthlyFee();
	}
}
