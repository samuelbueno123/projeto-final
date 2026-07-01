package com.cefet_rj.projeto_final.packages.states;

import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;

public class PackageExpectedState implements PackageStatusInterface {

    @Override
    public String getStatusName() {
        return "EXPECTED";
    }

	@Override
	public void raiseToNextState(Package pkg) {
		pkg.setStatus(PackageStatus.RECEIVED);
	}

}
