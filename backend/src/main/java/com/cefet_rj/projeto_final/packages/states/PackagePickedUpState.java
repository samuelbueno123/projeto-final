package com.cefet_rj.projeto_final.packages.states;

import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.packages.entities.Package;

public class PackagePickedUpState implements PackageStatusInterface {


    @Override
    public String getStatusName() {
        return "PICKED_UP";
    }

	@Override
	public void raiseToNextState(Package pkg) {
        // There is no next state after PICKED_UP, so we can leave this method empty or throw an exception
        throw new UnsupportedOperationException("No next state after PICKED_UP");
    }
}
