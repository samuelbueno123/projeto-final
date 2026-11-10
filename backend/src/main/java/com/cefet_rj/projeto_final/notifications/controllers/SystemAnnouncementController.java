package com.cefet_rj.projeto_final.notifications.controllers;

import com.cefet_rj.projeto_final.notifications.dtos.SystemAnnouncementRequestDTO;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/system-announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemAnnouncementController {
    private final NotificationService notificationService;
    private final com.cefet_rj.projeto_final.shared.observability.AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<Integer> publish(@Valid @RequestBody SystemAnnouncementRequestDTO request) {
        int recipients = notificationService.publishSystemAnnouncement(request);
        auditLogService.logAdminOperation(
                null,
                "BROADCAST_SYSTEM_ANNOUNCEMENT",
                "SYSTEM_ANNOUNCEMENT",
                "GLOBAL",
                null,
                null,
                true,
                "Comunicado de sistema enviado para " + recipients + " destinatário(s): " + request.title()
        );
        return ResponseEntity.ok(recipients);
    }
}

