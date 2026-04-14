package com.cefet_rj.projeto_final.contracts;

public class PendingPackage implements PackageStatusInterface {


    @Override
    public String getStatus() {
        return "PENDING";
    }

    @Override
    public void raiseToExpected() {

    }

    @Override
    public void raiseToPending() {

    }

    @Override
    public void raiseToPickedUp() {

    }
}
