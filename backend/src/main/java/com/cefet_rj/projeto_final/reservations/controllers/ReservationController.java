package com.cefet_rj.projeto_final.reservations.controllers;

import com.cefet_rj.projeto_final.reservations.dtos.ReservationDTO;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationRequestDTO;
import com.cefet_rj.projeto_final.reservations.services.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping(
            "/common-areas/{commonAreaId}/reservations"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationDTO> create(
            @PathVariable Long commonAreaId,
            @Valid
            @RequestBody ReservationRequestDTO dto
    ) {

        ReservationDTO created =
                reservationService.create(
                        commonAreaId,
                        dto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @GetMapping(
            "/condominiums/{condominiumId}/reservations/my"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservationDTO>>
    findMine(
            @PathVariable Long condominiumId
    ) {

        return ResponseEntity.ok(
                reservationService
                        .findMineByCondominium(
                                condominiumId
                        )
        );
    }

    @PatchMapping(
            "/reservations/{id}/cancel"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id
    ) {

        reservationService.cancel(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}