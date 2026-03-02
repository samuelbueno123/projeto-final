package com.cefet_rj.projeto_final.controllers.exceptions;

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
