package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.ResidentInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.ResidentUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.services.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

	@GetMapping
	public ResponseEntity<Page<ResidentDTO>> findAllPaged(Pageable pageable) {
		Page<ResidentDTO> list = residentService.findAllPaged(pageable);
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ResidentDTO> findById(@PathVariable Long id) {
		ResidentDTO dto = residentService.findById(id);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping
	public ResponseEntity<ResidentDTO> insert(@Valid @RequestBody ResidentInsertDTO dto){
		ResidentDTO newDto = residentService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(newDto.id()).toUri();

		return ResponseEntity.created(uri).body(newDto);
	}

	@PutMapping ("/{id}")
	public ResponseEntity<ResidentDTO> update(@PathVariable Long id, @Valid @RequestBody ResidentUpdateDTO dto){
		ResidentDTO newDto = residentService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		residentService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
