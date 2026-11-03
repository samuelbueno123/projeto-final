package com.cefet_rj.projeto_final.realtime.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.users.entities.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/realtime")
@RequiredArgsConstructor
@Tag(name = "Realtime", description = "Server-Sent Events (SSE) para atualização em tempo real de mensagens, avisos, notificações e encomendas")
public class RealtimeController {

    private final RealtimeEmitterService realtimeEmitterService;
    private final AuthenticatedUserService authenticatedUserService;
    private final MembershipRepository membershipRepository;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Canal SSE de eventos em tempo real para sincronização da interface com o banco de dados")
    public SseEmitter subscribeToRealtimeEvents() {
        User user = authenticatedUserService.getAuthenticatedUser();
        List<Long> condoIds = membershipRepository.findAllByUserId(user.getId()).stream()
                .map(m -> m.getCondominium().getId())
                .toList();

        return realtimeEmitterService.registerEmitter(user.getId(), condoIds);
    }
}
