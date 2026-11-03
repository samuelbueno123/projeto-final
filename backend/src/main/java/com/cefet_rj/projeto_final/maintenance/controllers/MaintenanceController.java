package com.cefet_rj.projeto_final.maintenance.controllers;

import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.CreateMaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.UpdateStatusDTO;
import com.cefet_rj.projeto_final.maintenance.services.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping("/condominiums/{condominiumId}/maintenance/orders")
    public ResponseEntity<List<MaintenanceOrderDTO>> listOrders(
            @PathVariable Long condominiumId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(maintenanceService.listOrders(condominiumId, status));
    }

    @GetMapping("/maintenance/orders/{id}")
    public ResponseEntity<MaintenanceOrderDTO> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getOrderById(id));
    }

    @PostMapping("/condominiums/{condominiumId}/maintenance/orders")
    public ResponseEntity<MaintenanceOrderDTO> createOrder(
            @PathVariable Long condominiumId,
            @Valid @RequestBody CreateMaintenanceOrderDTO dto) {
        MaintenanceOrderDTO created = maintenanceService.createOrder(condominiumId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/maintenance/orders/{id}/status")
    public ResponseEntity<MaintenanceOrderDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusDTO dto) {
        return ResponseEntity.ok(maintenanceService.updateStatus(id, dto));
    }
}
