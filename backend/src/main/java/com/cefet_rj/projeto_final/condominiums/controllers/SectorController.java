package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.SectorDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.SectorUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.services.SectorService;
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
import java.util.Map;

@RestController
@RequestMapping("/sectors")
@RequiredArgsConstructor
@Tag(
		name = "Sectors",
		description = "Blocos, torres e setores dos condomínios"
)
public class SectorController {

	private final SectorService sectorService;

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<?> findAll(
			@RequestParam(value = "condominiumId", required = false) Long condominiumId,
			Pageable pageable){
		if (condominiumId != null) {
			return ResponseEntity.ok().body(sectorService.findByCondominiumId(condominiumId));
		}
		Page<SectorDTO> list = sectorService.findAllPaged(pageable);
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<SectorDTO> findById(@PathVariable Long id){
		SectorDTO dto = sectorService.findById(id);
		return ResponseEntity.ok().body(dto);
	}
	
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<SectorDTO> insert(@Valid @RequestBody SectorInsertDTO dto){
		SectorDTO newDto = sectorService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
		                                     .buildAndExpand(newDto.id()).toUri();

		return ResponseEntity.created(uri).body(newDto);
	}

	@PutMapping ("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<SectorDTO> update(@PathVariable Long id, @Valid @RequestBody SectorUpdateDTO dto){
		SectorDTO newDto = sectorService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

	@PatchMapping("/{id}/image")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<SectorDTO> updateImage(@PathVariable Long id, @RequestBody Map<String, String> body) {
		String imageUrl = body != null ? body.get("imageUrl") : null;
		SectorDTO newDto = sectorService.updateImageUrl(id, imageUrl);
		return ResponseEntity.ok().body(newDto);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		sectorService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
