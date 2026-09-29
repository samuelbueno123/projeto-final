package com.cefet_rj.projeto_final.packages.history.controllers;

import com.cefet_rj.projeto_final.packages.history.dtos.PackageHistoryDTO;
import com.cefet_rj.projeto_final.packages.history.services.PackageHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/packages/{packageId}/history")
@RequiredArgsConstructor
public class PackageHistoryController {

    private final PackageHistoryService packageHistoryService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @GetMapping
    public ResponseEntity<List<PackageHistoryDTO>> findByPackageId(
            @PathVariable Long packageId
    ) {
        return ResponseEntity.ok(
                packageHistoryService.findByPackageId(packageId)
        );
    }
}