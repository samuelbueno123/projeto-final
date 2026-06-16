package com.cefet_rj.projeto_final.factories;

import com.cefet_rj.projeto_final.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.entities.enums.PackageStatus;
import com.cefet_rj.projeto_final.states.PackageExpectedState;
import com.cefet_rj.projeto_final.states.PackagePendingState;
import com.cefet_rj.projeto_final.states.PackagePickedUpState;

public class PackageStateFactory {

	public static PackageStatusInterface getState(PackageStatus status) {
		return switch (status)
		{
			case EXPECTED -> new PackageExpectedState();
			case PENDING -> new PackagePendingState();
			case PICKED_UP -> new PackagePickedUpState();
		};
	}
}