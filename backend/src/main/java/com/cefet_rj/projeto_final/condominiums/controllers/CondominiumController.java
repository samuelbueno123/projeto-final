package com.cefet_rj.projeto_final.condominiums.controllers;

import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.CondominiumInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.CondominiumUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.services.CondominiumService;
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

	@PostMapping
	public ResponseEntity<CondominiumDTO> insert(@Valid @RequestBody CondominiumInsertDTO dto){
		CondominiumDTO newDto = condominiumService.insert(dto);
		URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(newDto.id()).toUri();

		return ResponseEntity.created(uri).body(newDto);
	}

	@PutMapping ("/{id}")
	public ResponseEntity<CondominiumDTO> update(@PathVariable Long id, @Valid @RequestBody CondominiumUpdateDTO dto){
		CondominiumDTO newDto = condominiumService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		condominiumService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
