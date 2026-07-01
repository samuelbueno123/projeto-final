package com.cefet_rj.projeto_final.billing.contracts;

import java.math.BigDecimal;

public interface BillingPlanInterface {

	String getPlanName();

	BigDecimal calculateMonthlyFee();

}
