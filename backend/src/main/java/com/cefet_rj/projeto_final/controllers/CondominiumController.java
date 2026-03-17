package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.dtos.CondominiumUserDTO;
import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.services.CondominiumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/condominiums")
@RequiredArgsConstructor
public class CondominiumController {

    private final CondominiumService condominiumService;

    @GetMapping
    public ResponseEntity<Page<CondominiumDTO>> findAll(Pageable pageable){
        Page<CondominiumDTO> list = condominiumService.findAllPaged(pageable);
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CondominiumDTO> findById(@PathVariable Long id){
        CondominiumDTO dto = condominiumService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

	@PutMapping ("/{id}")
	public ResponseEntity<CondominiumDTO> update(@PathVariable Long id, @Valid @RequestBody CondominiumDTO dto){
		CondominiumDTO newDto = condominiumService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

// implementar autorização de role
	@PostMapping
	public ResponseEntity<CondominiumDTO> insert(@RequestBody CondominiumDTO dto){
		dto = condominiumService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
		                                     .buildAndExpand(dto.id()).toUri();

		return ResponseEntity.created(uri).body(dto);
	}
}
