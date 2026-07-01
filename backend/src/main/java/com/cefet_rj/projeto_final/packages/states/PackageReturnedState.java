package com.cefet_rj.projeto_final.packages.states;

import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.packages.entities.Package;

public class PackageReturnedState implements PackageStatusInterface {


    @Override
    public String getStatusName() {
        return "RETURNED";
    }

	@Override
	public void raiseToNextState(Package pkg) {
        // There is no next state after RETURNED, so we can leave this method empty or throw an exception
        throw new UnsupportedOperationException("No next state after RETURNED");
    }
}
