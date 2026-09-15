package com.cefet_rj.projeto_final.packages.exceptions;

public class InvalidPackageStateTransitionException extends RuntimeException {
    public InvalidPackageStateTransitionException(String msg){
        super(msg);
    }
}
