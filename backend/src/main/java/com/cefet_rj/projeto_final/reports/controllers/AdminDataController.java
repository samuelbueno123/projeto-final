package com.cefet_rj.projeto_final.reports.controllers;

import com.cefet_rj.projeto_final.reports.dtos.AdminDataSchemaDTO.*;
import com.cefet_rj.projeto_final.reports.services.AdminDataManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/data")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDataController {

    private final AdminDataManagementService dataManagementService;

    @GetMapping("/resources")
    public ResponseEntity<List<AdminResourceMetaDTO>> listResources() {
        return ResponseEntity.ok(dataManagementService.listResourcesMetadata());
    }

    @GetMapping("/{resource}/schema")
    public ResponseEntity<ResourceSchemaDTO> getSchema(@PathVariable String resource) {
        return ResponseEntity.ok(dataManagementService.getSchema(resource));
    }

    @GetMapping("/{resource}/{id}")
    public ResponseEntity<Map<String, Object>> getRecord(
            @PathVariable String resource,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(dataManagementService.getRecord(resource, id));
    }

    @PostMapping("/{resource}")
    public ResponseEntity<AdminRecordMutationResponse> createRecord(
            @PathVariable String resource,
            @RequestBody AdminRecordMutationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dataManagementService.createRecord(resource, request));
    }

    @PutMapping("/{resource}/{id}")
    public ResponseEntity<AdminRecordMutationResponse> updateRecord(
            @PathVariable String resource,
            @PathVariable Long id,
            @RequestBody AdminRecordMutationRequest request
    ) {
        return ResponseEntity.ok(dataManagementService.updateRecord(resource, id, request));
    }

    @DeleteMapping("/{resource}/{id}")
    public ResponseEntity<AdminRecordMutationResponse> deleteRecord(
            @PathVariable String resource,
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(dataManagementService.deleteRecord(resource, id, reason));
    }
}
