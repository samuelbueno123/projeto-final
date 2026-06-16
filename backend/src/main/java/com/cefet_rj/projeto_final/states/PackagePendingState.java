package com.cefet_rj.projeto_final.states;

import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.entities.enums.PackageStatus;

public class PackagePendingState implements PackageStatusInterface {

	@Override
	public String getStatusName() {
		return "PENDING";
	}

	@Override
	public void raiseToNextState(Package pkg) {
		pkg.setStatus(PackageStatus.PICKED_UP);
	}

}
