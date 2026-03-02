package com.cefet_rj.projeto_final.controllers.exceptions;

public record FieldMessage(
        String fieldName,
        String message
) {}
