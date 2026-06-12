package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.dtos.insert.PackageInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.PackageUpdateDTO;
import com.cefet_rj.projeto_final.services.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public ResponseEntity<Page<PackageDTO>> findAll(
            @RequestParam(value = "search", defaultValue = "") String search,
            Pageable pageable
    ) {
        Page<PackageDTO> list = packageService.findAllPaged(search, pageable);
        return ResponseEntity.ok().body(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackageDTO> findById(@PathVariable Long id){
        PackageDTO dto = packageService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    @PostMapping
    public ResponseEntity<PackageDTO> insert(@RequestBody PackageInsertDTO dto){
        PackageDTO newDto = packageService.insert(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newDto.id()).toUri();

        return ResponseEntity.created(uri).body(newDto);
    }

	@PutMapping ("/{id}")
	public ResponseEntity<PackageDTO> update(@PathVariable Long id, @Valid @RequestBody PackageUpdateDTO dto){
		PackageDTO newDto = packageService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        packageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
