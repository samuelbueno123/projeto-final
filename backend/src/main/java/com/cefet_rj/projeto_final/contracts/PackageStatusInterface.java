package com.cefet_rj.projeto_final.contracts;

import com.cefet_rj.projeto_final.entities.Package;

public interface PackageStatusInterface {

	String getStatusName();

	void raiseToNextState(Package pkg);

}
