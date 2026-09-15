package com.cefet_rj.projeto_final.reservations.controllers;

import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaDTO;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaRequestDTO;
import com.cefet_rj.projeto_final.reservations.services.CommonAreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaUpdateDTO;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Common Areas",
        description = "Gerenciamento das áreas comuns reserváveis"
)
public class CommonAreaController {

    private final CommonAreaService commonAreaService;

    @PostMapping(
            "/condominiums/{condominiumId}/common-areas"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER')"
    )
    public ResponseEntity<CommonAreaDTO> create(
            @PathVariable Long condominiumId,
            @Valid
            @RequestBody CommonAreaRequestDTO dto
    ) {

        CommonAreaDTO created =
                commonAreaService.create(
                        condominiumId,
                        dto
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @GetMapping(
            "/condominiums/{condominiumId}/common-areas"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommonAreaDTO>>
    findAll(
            @PathVariable Long condominiumId
    ) {

        return ResponseEntity.ok(
                commonAreaService
                        .findActiveByCondominium(
                                condominiumId
                        )
        );
    }

    @PutMapping(
            "/common-areas/{id}"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER')"
    )
    public ResponseEntity<CommonAreaDTO> update(
            @PathVariable Long id,
            @Valid
            @RequestBody CommonAreaUpdateDTO dto
    ) {

        return ResponseEntity.ok(
                commonAreaService.update(
                        id,
                        dto
                )
        );
    }

    @PatchMapping(
            "/common-areas/{id}/activate"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER')"
    )
    public ResponseEntity<Void> activate(
            @PathVariable Long id
    ) {

        commonAreaService.activate(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping(
            "/common-areas/{id}/deactivate"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER')"
    )
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id
    ) {

        commonAreaService.deactivate(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}