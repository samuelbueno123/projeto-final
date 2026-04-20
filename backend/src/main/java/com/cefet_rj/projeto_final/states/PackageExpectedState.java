package com.cefet_rj.projeto_final.states;

import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.contracts.PackageStatusInterface;

public class PackageExpectedState implements PackageStatusInterface {

    @Override
    public String getStatusName() {
        return "EXPECTED";
    }

	@Override
	public void raiseToNextState(Package pkg) {
		pkg.setStatus(new PackagePendingState());
	}

}
