package com.cefet_rj.projeto_final.interfaces;

import lombok.Getter;
import lombok.Setter;

public class PremiumBillingPlan implements CondominiumBillingPlan {

	@Getter @Setter
	private Double monthlyFee;

	@Getter @Setter
	private Double pickupFee;

	public PremiumBillingPlan() {
		this.monthlyFee = 200.0; // Exemplo de taxa mensal fixa para o plano premium
		this.pickupFee = 20.0; // Exemplo de taxa por coleta para o plano premium
	}

	@Override
	public Boolean isBasicPlan() { return false; }

	@Override
	public Boolean isPremiumPlan() { return true; }

}
