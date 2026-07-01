package com.cefet_rj.projeto_final.shared.exceptions;

public class InvalidPackageStateTransitionException extends RuntimeException {

    public InvalidPackageStateTransitionException(String message) {
        super(message);
    }
}
