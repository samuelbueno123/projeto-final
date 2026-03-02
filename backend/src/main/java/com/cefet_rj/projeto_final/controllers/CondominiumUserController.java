package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.CondominiumUserDTO;
import com.cefet_rj.projeto_final.services.CondominiumUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/condominium-users")
@RequiredArgsConstructor
public class CondominiumUserController {

	private final CondominiumUserService condominiumUserService;

	@GetMapping
	public ResponseEntity <Page<CondominiumUserDTO>> findAll(Pageable pageable){
		Page<CondominiumUserDTO> list = condominiumUserService.findAllPaged(pageable);
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CondominiumUserDTO> findById(@PathVariable Long id){
		CondominiumUserDTO dto = condominiumUserService.findById(id);
		return ResponseEntity.ok().body(dto);
	}

}
