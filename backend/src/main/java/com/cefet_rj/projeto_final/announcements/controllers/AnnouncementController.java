package com.cefet_rj.projeto_final.announcements.controllers;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.announcements.services.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementUpdateDTO;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Announcements",
        description = "Comunicados dos condomínios"
)
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping("/condominiums/{condominiumId}/announcements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AnnouncementDTO> create(
            @PathVariable Long condominiumId,
            @Valid @RequestBody AnnouncementRequestDTO dto
    ) {

        AnnouncementDTO created =
                announcementService.create(condominiumId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @GetMapping("/condominiums/{condominiumId}/announcements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AnnouncementDTO>> findAll(
            @PathVariable Long condominiumId
    ) {

        return ResponseEntity.ok(
                announcementService
                        .findActiveByCondominium(condominiumId)
        );
    }

    @PatchMapping("/announcements/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id
    ) {

        announcementService.deactivate(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/condominiums/{condominiumId}/announcements/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<AnnouncementDTO>> findAllForAdmin(
            @PathVariable Long condominiumId
    ) {

        return ResponseEntity.ok(
                announcementService
                        .findAllByCondominiumForAdmin(
                                condominiumId
                        )
        );
    }

    @PutMapping("/announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AnnouncementDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateDTO dto
    ) {

        return ResponseEntity.ok(
                announcementService.update(id, dto)
        );
    }

    @PatchMapping("/announcements/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> activate(
            @PathVariable Long id
    ) {

        announcementService.activate(id);

        return ResponseEntity.noContent().build();
    }
}