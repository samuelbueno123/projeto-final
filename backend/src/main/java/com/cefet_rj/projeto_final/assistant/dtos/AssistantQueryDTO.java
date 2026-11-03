package com.cefet_rj.projeto_final.assistant.dtos;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AssistantQueryDTO(
        @NotBlank String question,
        String category
) {
    public record AssistantResponseDTO(
            String question,
            String answer,
            String legalBasis,
            List<String> suggestedFollowUps
    ) {}
}
