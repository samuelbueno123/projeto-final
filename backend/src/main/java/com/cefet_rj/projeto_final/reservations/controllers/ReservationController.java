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
        description = "Reservas, fila de espera e solicitações de reserva de áreas comuns"
)
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping(
            "/common-areas/{commonAreaId}/reservations"
    )
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Solicitar ou criar reserva (ou entrar na fila de espera quando joinWaitlist=true)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Reserva, solicitação ou inscrição na lista de espera criada"
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
        ReservationDTO created = reservationService.create(commonAreaId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/common-areas/{commonAreaId}/waitlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Entrar na lista de espera de uma área comum indisponível")
    public ResponseEntity<ReservationDTO> joinWaitlist(
            @PathVariable Long commonAreaId,
            @Valid @RequestBody ReservationRequestDTO dto
    ) {
        ReservationDTO created = reservationService.joinWaitlist(commonAreaId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/common-areas/{commonAreaId}/waitlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar fila de espera de uma área comum")
    public ResponseEntity<List<ReservationDTO>> findWaitlist(
            @PathVariable Long commonAreaId
    ) {
        return ResponseEntity.ok(reservationService.findWaitlistByCommonArea(commonAreaId));
    }

    @GetMapping(
            "/condominiums/{condominiumId}/reservations/my"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservationDTO>> findMine(
            @PathVariable Long condominiumId
    ) {
        return ResponseEntity.ok(reservationService.findMineByCondominium(condominiumId));
    }

    @PatchMapping("/reservations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @Operation(summary = "Porteiro ou síndico aprova a reserva solicitada")
    public ResponseEntity<ReservationDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.approve(id));
    }

    @PatchMapping("/reservations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @Operation(summary = "Porteiro ou síndico recusa a solicitação de reserva")
    public ResponseEntity<ReservationDTO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(reservationService.reject(id, reason));
    }

    @PatchMapping("/reservations/{id}/counter-propose")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @Operation(summary = "Porteiro ou síndico envia uma contraproposta de horário")
    public ResponseEntity<ReservationDTO> counterPropose(
            @PathVariable Long id,
            @Valid @RequestBody ReservationDTO.CounterProposalDTO dto
    ) {
        return ResponseEntity.ok(reservationService.counterPropose(id, dto));
    }

    @PatchMapping("/reservations/{id}/modify-and-confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @Operation(summary = "Porteiro ou síndico altera horário/especificações e salva direto como confirmada")
    public ResponseEntity<ReservationDTO> modifyAndConfirm(
            @PathVariable Long id,
            @Valid @RequestBody ReservationDTO.ModifyAndConfirmDTO dto
    ) {
        return ResponseEntity.ok(reservationService.modifyAndConfirm(id, dto));
    }

    @PatchMapping("/reservations/{id}/accept-counter-proposal")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Morador aceita a contraproposta da portaria")
    public ResponseEntity<ReservationDTO> acceptCounterProposal(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.acceptCounterProposal(id));
    }

    @PatchMapping("/reservations/{id}/decline-counter-proposal")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Morador recusa a contraproposta da portaria")
    public ResponseEntity<ReservationDTO> declineCounterProposal(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.declineCounterProposal(id));
    }

    @PatchMapping(
            "/reservations/{id}/cancel"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id
    ) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(
            "/condominiums/{condominiumId}/reservations"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')"
    )
    @Operation(summary = "Portaria ou administração lista todas as reservas do condomínio")
    public ResponseEntity<List<ReservationDTO>> findAllByCondominium(
            @PathVariable Long condominiumId
    ) {
        return ResponseEntity.ok(reservationService.findAllByCondominium(condominiumId));
    }
}
