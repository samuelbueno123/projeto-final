package com.cefet_rj.projeto_final.assistant.controllers;

import com.cefet_rj.projeto_final.assistant.dtos.AssistantQueryDTO;
import com.cefet_rj.projeto_final.assistant.dtos.AssistantQueryDTO.AssistantResponseDTO;
import com.cefet_rj.projeto_final.assistant.services.CondoAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CondoAssistantController {

    private final CondoAssistantService assistantService;

    @PostMapping("/condominiums/{condominiumId}/assistant/query")
    public ResponseEntity<AssistantResponseDTO> queryAssistant(
            @PathVariable Long condominiumId,
            @Valid @RequestBody AssistantQueryDTO query) {
        return ResponseEntity.ok(assistantService.answerQuestion(condominiumId, query));
    }
}
