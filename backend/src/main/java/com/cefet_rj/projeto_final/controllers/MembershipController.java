package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.services.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/condominium-users")
@RequiredArgsConstructor
public class MembershipController {

	private final MembershipService condominiumUserService;

	@GetMapping
	public ResponseEntity <Page<MembershipDTO>> findAll(Pageable pageable){
		Page<MembershipDTO> list = condominiumUserService.findAllPaged(pageable);
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MembershipDTO> findById(@PathVariable Long id){
		MembershipDTO dto = condominiumUserService.findById(id);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping
	public ResponseEntity<MembershipDTO> insert(@RequestBody MembershipDTO dto){
		dto = condominiumUserService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(dto.id()).toUri();

		return ResponseEntity.created(uri).body(dto);
	}

	@PutMapping ("/{id}")
	public ResponseEntity<MembershipDTO> update(@PathVariable Long id, @Valid @RequestBody MembershipDTO dto){
		MembershipDTO newDto = condominiumUserService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}
}
