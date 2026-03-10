package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.services.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

	@PutMapping ("/{id}")
	public ResponseEntity<ResidentDTO> update(@PathVariable Long id, @Valid @RequestBody ResidentDTO dto){
		ResidentDTO newDto = residentService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}
}
