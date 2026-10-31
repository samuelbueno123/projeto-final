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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Reservations",
        description = "Reservas e solicitações de reserva de áreas comuns"
)
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping(
            "/common-areas/{commonAreaId}/reservations"
    )
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Solicitar ou criar reserva",
            description = "Solicita ou cria uma reserva para uma área comum disponível."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Reserva ou solicitação criada"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Existe outra reserva conflitante no período"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário sem acesso ou feature indisponível"
            )
    })
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

    @PatchMapping("/reservations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ReservationDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.approve(id));
    }

    @PatchMapping("/reservations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ReservationDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(reservationService.reject(id, reason));
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

    @GetMapping(
            "/condominiums/{condominiumId}/reservations"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER')"
    )
    public ResponseEntity<List<ReservationDTO>>
    findAllByCondominium(
            @PathVariable Long condominiumId
    ) {

        return ResponseEntity.ok(
                reservationService
                        .findAllByCondominium(
                                condominiumId
                        )
        );
    }
}
