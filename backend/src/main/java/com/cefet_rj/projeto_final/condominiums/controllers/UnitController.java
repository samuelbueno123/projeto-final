package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.UnitDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.UnitInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.UnitUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.services.UnitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<Page<UnitDTO>> findAll(Pageable pageable){
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
