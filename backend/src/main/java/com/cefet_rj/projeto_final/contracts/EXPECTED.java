package com.cefet_rj.projeto_final.contracts;

public class ExpectedPackage implements PackageStatusInterface {


    @Override
    public String getStatus() {
        return "EXPECTED";
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
