package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.services.SectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/sectors")
@RequiredArgsConstructor
public class SectorController {

	private final SectorService sectorService;

	@GetMapping
	public ResponseEntity<Page<SectorDTO>> findAll(Pageable pageable){
		Page<SectorDTO> list = sectorService.findAllPaged(pageable);
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("/{id}")
	public ResponseEntity<SectorDTO> findById(@PathVariable Long id){
		SectorDTO dto = sectorService.findById(id);
		return ResponseEntity.ok().body(dto);
	}
	// implementar autorização de role
	@PostMapping
	public ResponseEntity<SectorDTO> insert(@RequestBody SectorDTO dto){
		dto = sectorService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
		                                     .buildAndExpand(dto.id()).toUri();

		return ResponseEntity.created(uri).body(dto);
	}
}
