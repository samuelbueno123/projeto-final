package com.cefet_rj.projeto_final.community.controllers;

import com.cefet_rj.projeto_final.community.dtos.ChatConversationDTO;
import com.cefet_rj.projeto_final.community.dtos.ChatMessageDTO;
import com.cefet_rj.projeto_final.community.services.DoormanChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/condominiums/{condominiumId}/doorman-chat")
@RequiredArgsConstructor
@Tag(name = "Doorman Chat", description = "Chat em tempo real com a portaria")
public class DoormanChatController {

    private final DoormanChatService doormanChatService;

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @Operation(summary = "Lista conversas de moradores para a portaria selecionar")
    public ResponseEntity<List<ChatConversationDTO>> getConversations(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(doormanChatService.findConversations(condominiumId));
    }

    @GetMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista mensagens do chat com a portaria")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) Long targetUserId
    ) {
        return ResponseEntity.ok(doormanChatService.findMessages(condominiumId, targetUserId));
    }

    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Envia mensagem no chat da portaria")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long condominiumId,
            @Valid @RequestBody ChatMessageDTO.SendMessageDTO dto
    ) {
        ChatMessageDTO sent = doormanChatService.sendMessage(condominiumId, dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(sent.id())
                .toUri();
        return ResponseEntity.created(uri).body(sent);
    }

    @PatchMapping("/messages/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marca mensagens recebidas como lidas")
    public ResponseEntity<Void> markAsRead(@PathVariable Long condominiumId) {
        doormanChatService.markMessagesAsRead(condominiumId);
        return ResponseEntity.noContent().build();
    }
}
