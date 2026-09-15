package com.cefet_rj.projeto_final.shared.exceptionhandler;

import java.time.Instant;
import java.util.List;

public record ValidationError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldMessage> errors
) {}
