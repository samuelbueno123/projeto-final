package com.cefet_rj.projeto_final.notifications.controllers;

import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(
        name = "Notifications",
        description = "Notificações do usuário autenticado"
)
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    @Operation(summary = "Lista todas as notificações do usuário")
    public ResponseEntity<List<NotificationDTO>> findMyNotifications() {
        return ResponseEntity.ok(
                notificationService.findMyNotifications()
        );
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my/unread-count")
    @Operation(summary = "Retorna a contagem rápida de notificações não lidas")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.countUnreadNotifications()));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/read-all")
    @Operation(summary = "Marca todas as notificações do usuário como lidas")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca uma notificação específica como lida")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                notificationService.markAsRead(id)
        );
    }
}
