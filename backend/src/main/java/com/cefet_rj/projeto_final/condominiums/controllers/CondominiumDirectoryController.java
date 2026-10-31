package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.dtos.DirectoryResidentDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.DirectoryUnitDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Diretório de unidades e moradores de um condomínio, usado pela portaria e
 * pelo síndico para localizar rapidamente destinatários (encomendas e comunicados).
 */
@RestController
@RequestMapping("/condominiums/{condominiumId}/directory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
@Tag(
        name = "Directory",
        description = "Hierarquia de setores, unidades e moradores do condomínio"
)
public class CondominiumDirectoryController {

    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;

    @GetMapping("/units")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DirectoryUnitDTO>> units(@PathVariable Long condominiumId) {
        validateAccess(condominiumId);
        return ResponseEntity.ok(
                unitRepository.findAllForCondominium(condominiumId)
                        .stream()
                        .map(DirectoryUnitDTO::new)
                        .toList()
        );
    }

    @GetMapping("/units/{unitId}/residents")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DirectoryResidentDTO>> residents(
            @PathVariable Long condominiumId,
            @PathVariable Long unitId
    ) {
        validateAccess(condominiumId);

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));

        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new ResourceNotFoundException("Unit not found in this condominium");
        }

        return ResponseEntity.ok(
                residentRepository.findAllByUnitIdWithUser(unitId)
                        .stream()
                        .map(DirectoryResidentDTO::new)
                        .toList()
        );
    }

    private void validateAccess(Long condominiumId) {
        User user = authenticatedUserService.getAuthenticatedUser();
        if (user.hasRole("ROLE_ADMIN")) {
            return;
        }
        condominiumAccessService.validateUserBelongsToCondominium(user.getId(), condominiumId);
    }
}
