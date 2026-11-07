package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumGalleryDTO;
import com.cefet_rj.projeto_final.condominiums.services.CondominiumGalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/condominiums/{condominiumId}/gallery")
@RequiredArgsConstructor
@Tag(name = "Condominium Gallery", description = "Galeria de imagens do condomínio e áreas comuns")
public class CondominiumGalleryController {

    private final CondominiumGalleryService galleryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista todas as imagens da galeria do condomínio")
    public ResponseEntity<List<CondominiumGalleryDTO>> findByCondominium(
            @PathVariable Long condominiumId
    ) {
        return ResponseEntity.ok(galleryService.findByCondominium(condominiumId));
    }

    @GetMapping("/area/{areaId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista imagens de uma área comum específica")
    public ResponseEntity<List<CondominiumGalleryDTO>> findByArea(
            @PathVariable Long condominiumId,
            @PathVariable Long areaId
    ) {
        return ResponseEntity.ok(galleryService.findByCommonArea(condominiumId, areaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Síndico envia uma nova imagem para a galeria do condomínio")
    public ResponseEntity<CondominiumGalleryDTO> addImage(
            @PathVariable Long condominiumId,
            @Valid @RequestBody CondominiumGalleryDTO.CreateGalleryImageDTO dto
    ) {
        CondominiumGalleryDTO created = galleryService.addImage(condominiumId, dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(uri).body(created);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove uma imagem da galeria do condomínio")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long condominiumId,
            @PathVariable Long imageId
    ) {
        galleryService.deleteImage(condominiumId, imageId);
        return ResponseEntity.noContent().build();
    }
}
