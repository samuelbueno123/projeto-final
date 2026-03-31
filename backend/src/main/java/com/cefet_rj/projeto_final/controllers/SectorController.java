package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.SectorUpdateDTO;
import com.cefet_rj.projeto_final.services.SectorService;
import jakarta.validation.Valid;
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
	
	@PostMapping
	public ResponseEntity<SectorDTO> insert(@RequestBody SectorInsertDTO dto){
		SectorDTO newDto = sectorService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
		                                     .buildAndExpand(newDto.id()).toUri();

		return ResponseEntity.created(uri).body(newDto);
	}

	@PutMapping ("/{id}")
	public ResponseEntity<SectorDTO> update(@PathVariable Long id, @Valid @RequestBody SectorUpdateDTO dto){
		SectorDTO newDto = sectorService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}
}
