package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.services.CondominiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
