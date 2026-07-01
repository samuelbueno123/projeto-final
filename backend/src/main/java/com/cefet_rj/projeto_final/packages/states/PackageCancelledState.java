package com.cefet_rj.projeto_final.packages.states;

import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.packages.entities.Package;

public class PackageCancelledState implements PackageStatusInterface {


    @Override
    public String getStatusName() {
        return "CANCELLED";
    }

	@Override
	public void raiseToNextState(Package pkg) {
    }
}
