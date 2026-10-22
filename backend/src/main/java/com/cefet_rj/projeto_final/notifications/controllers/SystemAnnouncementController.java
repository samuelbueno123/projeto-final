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

    @PostMapping
    public ResponseEntity<Integer> publish(@Valid @RequestBody SystemAnnouncementRequestDTO request) {
        return ResponseEntity.ok(notificationService.publishSystemAnnouncement(request));
    }
}
