package com.cefet_rj.projeto_final.shared.exceptions;

public class InvalidStatusException extends RuntimeException {
    public InvalidStatusException(String msg) {
        super(msg);
    }
}