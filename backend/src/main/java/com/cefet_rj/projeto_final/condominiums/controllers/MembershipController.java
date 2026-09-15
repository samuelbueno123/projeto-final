package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.MembershipInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.MembershipUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.services.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/memberships")
@RequiredArgsConstructor
public class MembershipController {

	private final MembershipService membershipService;

	@GetMapping
	public ResponseEntity <Page<MembershipDTO>> findAll(Pageable pageable){
		Page<MembershipDTO> list = membershipService.findAllPaged(pageable);
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MembershipDTO> findById(@PathVariable Long id){
		MembershipDTO dto = membershipService.findById(id);
		return ResponseEntity.ok().body(dto);
	}

	@PostMapping
	public ResponseEntity<MembershipDTO> insert(@Valid @RequestBody MembershipInsertDTO dto){
		MembershipDTO newDto = membershipService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(newDto.id()).toUri();

		return ResponseEntity.created(uri).body(newDto);
	}

	@PutMapping ("/{id}")
	public ResponseEntity<MembershipDTO> update(@PathVariable Long id, @Valid @RequestBody MembershipUpdateDTO dto){
		MembershipDTO newDto = membershipService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		membershipService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
