package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.CondominiumUserDTO;
import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.services.CondominiumUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

	@PutMapping ("/{id}")
	public ResponseEntity<CondominiumUserDTO> update(@PathVariable Long id, @Valid @RequestBody CondominiumUserDTO dto){
		CondominiumUserDTO newDto = condominiumUserService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

}
