package com.cefet_rj.projeto_final.shared.exceptionhandler;

import java.time.Instant;

public record StandardError(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path
) {}
