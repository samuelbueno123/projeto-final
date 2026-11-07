package com.cefet_rj.projeto_final.maintenance.controllers;

import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.CreateMaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.UpdateStatusDTO;
import com.cefet_rj.projeto_final.maintenance.services.MaintenanceService;
import com.cefet_rj.projeto_final.maintenance.services.MaintenanceService.MaintenanceEquipmentDTO;
import com.cefet_rj.projeto_final.maintenance.services.MaintenanceService.SupplierDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
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

    // PREVENTIVE EQUIPMENT & SCHEDULES
    @GetMapping("/condominiums/{condominiumId}/maintenance/equipment")
    public ResponseEntity<List<MaintenanceEquipmentDTO>> listEquipment(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(maintenanceService.listEquipment(condominiumId));
    }

    @PostMapping("/condominiums/{condominiumId}/maintenance/equipment")
    public ResponseEntity<MaintenanceEquipmentDTO> createEquipment(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.createEquipment(condominiumId, body));
    }

    @PutMapping("/maintenance/equipment/{id}")
    public ResponseEntity<MaintenanceEquipmentDTO> updateEquipment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(maintenanceService.updateEquipment(id, body));
    }

    @DeleteMapping("/maintenance/equipment/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        maintenanceService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }

    // SUPPLIERS & SERVICE PROVIDERS
    @GetMapping("/condominiums/{condominiumId}/suppliers")
    public ResponseEntity<List<SupplierDTO>> listSuppliers(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(maintenanceService.listSuppliers(condominiumId));
    }

    @PostMapping("/condominiums/{condominiumId}/suppliers")
    public ResponseEntity<SupplierDTO> createSupplier(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maintenanceService.createSupplier(condominiumId, body));
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(maintenanceService.updateSupplier(id, body));
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        maintenanceService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
