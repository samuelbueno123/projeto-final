package com.cefet_rj.projeto_final.billing.controllers;

import com.cefet_rj.projeto_final.billing.dtos.FeatureDTO;
import com.cefet_rj.projeto_final.billing.services.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/billing/features")
@RequiredArgsConstructor
@Tag(
        name = "Features",
        description = "Catálogo de funcionalidades disponíveis"
)
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FeatureDTO>> findAll() {

        return ResponseEntity.ok(
                featureService.findAll()
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FeatureDTO> upsert(@RequestBody FeatureDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(featureService.upsert(dto));
    }
}
