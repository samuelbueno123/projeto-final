package com.cefet_rj.projeto_final.packages.states;

import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.packages.entities.Package;

public class PackageNotifiedState implements PackageStatusInterface {


    @Override
    public String getStatusName() {
        return "NOTIFIED";
    }

	@Override
	public void raiseToNextState(Package pkg) {pkg.setStatus(PackageStatus.PICKED_UP);}
}
