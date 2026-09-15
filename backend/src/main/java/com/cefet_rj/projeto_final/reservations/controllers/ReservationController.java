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

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Reservations",
        description = "Reservas de áreas comuns"
)
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping(
            "/common-areas/{commonAreaId}/reservations"
    )
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Criar reserva",
            description = "Cria uma reserva para uma área comum disponível."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Reserva criada"
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