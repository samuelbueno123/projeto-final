package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.UnitCsvImportResultDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.UnitDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.UnitInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.UnitUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.services.UnitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
@Tag(
        name = "Units",
        description = "Unidades residenciais dos condomínios"
)
public class UnitController {

    private final UnitService unitService;

    @GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> findAll(
            @RequestParam(value = "condominiumId", required = false) Long condominiumId,
            Pageable pageable){
        if (condominiumId != null) {
            return ResponseEntity.ok().body(unitService.findByCondominiumId(condominiumId));
        }
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Informe o parâmetro condominiumId para listar as unidades.");
        }
        Page<UnitDTO> list = unitService.findAllPaged(pageable);
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UnitDTO> findById(@PathVariable Long id){
        UnitDTO dto = unitService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    @PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UnitDTO> insert(@Valid @RequestBody UnitInsertDTO dto){
        UnitDTO newDto = unitService.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newDto.id()).toUri();

        return ResponseEntity.created(uri).body(newDto);
    }

    @PostMapping(value = "/condominiums/{condominiumId}/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UnitCsvImportResultDTO> importCsv(
            @PathVariable Long condominiumId,
            @RequestParam("file") MultipartFile file
    ) {
        UnitCsvImportResultDTO result = unitService.importCsv(condominiumId, file);
        return ResponseEntity.ok(result);
    }

    @PutMapping ("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<UnitDTO> update(@PathVariable Long id, @Valid @RequestBody UnitUpdateDTO dto){
		UnitDTO newDto = unitService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

    @DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
