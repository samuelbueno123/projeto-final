package com.cefet_rj.projeto_final.packages.contracts;

import com.cefet_rj.projeto_final.packages.entities.Package;

public interface PackageStatusInterface {

	String getStatusName();

	void raiseToNextState(Package pkg);

}
