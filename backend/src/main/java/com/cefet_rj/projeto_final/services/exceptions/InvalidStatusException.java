package com.cefet_rj.projeto_final.services.exceptions;

public class InvalidStatusException extends RuntimeException {
    public InvalidStatusException(String msg) {
        super(msg);
    }
}