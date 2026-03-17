package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.services.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public ResponseEntity<Page<PackageDTO>> findAll(Pageable pageable){
        Page<PackageDTO> list = packageService.findAllPaged(pageable);
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageDTO> findById(@PathVariable Long id){
        PackageDTO dto = packageService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

	@PutMapping ("/{id}")
	public ResponseEntity<PackageDTO> update(@PathVariable Long id, @Valid @RequestBody PackageDTO dto){
		PackageDTO newDto = packageService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}
}
