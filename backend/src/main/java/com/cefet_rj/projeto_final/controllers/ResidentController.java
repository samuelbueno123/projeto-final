package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.services.ResidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
