package com.cefet_rj.projeto_final.notifications.controllers;

import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasRole('RESIDENT')")
    @GetMapping("/my")
    public ResponseEntity<List<NotificationDTO>> findMyNotifications() {
        return ResponseEntity.ok(
                notificationService.findMyNotifications()
        );
    }

    @PreAuthorize("hasRole('RESIDENT')")
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                notificationService.markAsRead(id)
        );
    }
}