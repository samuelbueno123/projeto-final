package com.cefet_rj.projeto_final.packages.factories;

import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.states.*;

public class PackageStateFactory {

	public static PackageStatusInterface getState(PackageStatus status) {
		return switch (status)
		{
			case EXPECTED -> new PackageExpectedState();
			case RECEIVED -> new PackageReceivedState();
			case NOTIFIED -> new PackageNotifiedState();
			case PICKED_UP -> new PackagePickedUpState();
			case CANCELLED -> new PackageCancelledState();
			case RETURNED -> new PackageReturnedState();
		};
	}
}