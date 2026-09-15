package com.cefet_rj.projeto_final.shared.exceptionhandler;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class StandardError {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
}