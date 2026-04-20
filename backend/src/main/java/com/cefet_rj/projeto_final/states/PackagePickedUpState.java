package com.cefet_rj.projeto_final.states;

import com.cefet_rj.projeto_final.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.entities.Package;

public class PackagePickedUpState implements PackageStatusInterface {


    @Override
    public String getStatusName() {
        return "PICKED_UP";
    }

	@Override
	public void raiseToNextState(Package pkg) {
	}

}
