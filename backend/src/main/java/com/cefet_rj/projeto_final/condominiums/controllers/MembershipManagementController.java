package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.AddManagerRequestDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipInvitationDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipInvitationRequestDTO;
import com.cefet_rj.projeto_final.condominiums.services.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Memberships",
        description = "Convites, solicitações de entrada e gestão de vínculos por condomínio"
)
public class MembershipManagementController {

    private final MembershipService membershipService;

    @GetMapping("/condominiums/available")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar condomínios ativos para solicitação de vínculo")
    public ResponseEntity<List<MembershipService.AvailableCondominiumDTO>> listAvailableCondominiums() {
        return ResponseEntity.ok(membershipService.findAvailableCondominiums());
    }

    @GetMapping("/condominiums/{condominiumId}/available-units")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar disponibilidade de unidades de um condomínio")
    public ResponseEntity<List<MembershipService.AvailableUnitDTO>> listAvailableUnits(
            @PathVariable Long condominiumId
    ) {
        return ResponseEntity.ok(membershipService.findAvailableUnits(condominiumId));
    }

    @GetMapping("/membership-requests/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar meus convites e solicitações de vínculo pendentes")
    public ResponseEntity<List<MembershipDTO>> listMyRequests() {
        return ResponseEntity.ok(membershipService.findMyPendingAndRequests());
    }

    @PostMapping("/condominiums/{condominiumId}/membership-requests")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Enviar solicitação para vincular-se a um condomínio e unidade")
    public ResponseEntity<MembershipDTO> requestMembership(
            @PathVariable Long condominiumId,
            @RequestBody MembershipService.MembershipRequestCreateDTO dto
    ) {
        MembershipDTO created = membershipService.requestMembership(condominiumId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/manager/condominiums/{condominiumId}/membership-requests/{membershipId}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aprovar solicitação de vínculo de morador ou porteiro")
    public ResponseEntity<MembershipDTO> approveRequest(
            @PathVariable Long condominiumId,
            @PathVariable Long membershipId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Long unitId = null;
        String residentType = null;
        if (body != null) {
            Object rawUnit = body.get("unitId");
            if (rawUnit instanceof Number num) {
                unitId = num.longValue();
            }
            Object rawType = body.get("residentType");
            if (rawType instanceof String str) {
                residentType = str;
            }
        }
        return ResponseEntity.ok(
                membershipService.approveMembershipRequest(condominiumId, membershipId, unitId, residentType)
        );
    }

    @PostMapping("/manager/condominiums/{condominiumId}/membership-requests/{membershipId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Recusar solicitação de vínculo")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long condominiumId,
            @PathVariable Long membershipId
    ) {
        membershipService.rejectMembershipRequest(condominiumId, membershipId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/manager/condominiums/{condominiumId}/memberships/{membershipId}/unit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vincular ou alterar a unidade de um morador do condomínio")
    public ResponseEntity<MembershipDTO> assignUnit(
            @PathVariable Long condominiumId,
            @PathVariable Long membershipId,
            @RequestBody Map<String, Object> body
    ) {
        Long unitId = body.get("unitId") instanceof Number num ? num.longValue() : null;
        String residentType = body.get("residentType") instanceof String str ? str : "OWNER";
        return ResponseEntity.ok(
                membershipService.assignResidentUnit(condominiumId, membershipId, unitId, residentType)
        );
    }

    @GetMapping("/manager/condominiums/{condominiumId}/memberships")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar vínculos de um condomínio administrado")
    public ResponseEntity<List<MembershipDTO>> listCondominiumMemberships(
            @PathVariable Long condominiumId
    ) {
        return ResponseEntity.ok(membershipService.findByCondominiumForManager(condominiumId));
    }

    @PostMapping("/manager/condominiums/{condominiumId}/membership-invitations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Convidar usuário para um condomínio")
    public ResponseEntity<MembershipInvitationDTO> inviteMember(
            @PathVariable Long condominiumId,
            @Valid @RequestBody MembershipInvitationRequestDTO dto
    ) {
        MembershipInvitationDTO created = membershipService.inviteMember(condominiumId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/manager/condominiums/{condominiumId}/memberships/{membershipId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remover vínculo de porteiro ou morador de um condomínio")
    public ResponseEntity<Void> removeDoorman(
            @PathVariable Long condominiumId,
            @PathVariable Long membershipId
    ) {
        membershipService.removeDoorman(condominiumId, membershipId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/membership-invitations/{invitationId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Aceitar convite de vínculo recebido")
    public ResponseEntity<MembershipDTO> acceptInvitation(
            @PathVariable Long invitationId
    ) {
        return ResponseEntity.ok(membershipService.acceptInvitation(invitationId));
    }

    @PostMapping("/membership-invitations/{invitationId}/decline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Recusar convite de vínculo recebido")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable Long invitationId
    ) {
        membershipService.declineInvitation(invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/condominiums/{condominiumId}/managers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar síndicos de qualquer condomínio")
    public ResponseEntity<List<MembershipDTO>> listManagers(
            @PathVariable Long condominiumId
    ) {
        return ResponseEntity.ok(membershipService.findManagersByCondominiumForAdmin(condominiumId));
    }

    @PostMapping("/admin/condominiums/{condominiumId}/managers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adicionar síndico a qualquer condomínio")
    public ResponseEntity<MembershipDTO> addManager(
            @PathVariable Long condominiumId,
            @Valid @RequestBody AddManagerRequestDTO dto
    ) {
        MembershipDTO created = membershipService.addManagerForAdmin(condominiumId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/admin/condominiums/{condominiumId}/managers/{membershipId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover síndico de qualquer condomínio")
    public ResponseEntity<Void> removeManager(
            @PathVariable Long condominiumId,
            @PathVariable Long membershipId
    ) {
        membershipService.removeManagerForAdmin(condominiumId, membershipId);
        return ResponseEntity.noContent().build();
    }
}
