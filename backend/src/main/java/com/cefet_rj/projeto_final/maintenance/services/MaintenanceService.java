package com.cefet_rj.projeto_final.maintenance.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.CreateMaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.UpdateStatusDTO;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceOrder;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceOrderRepository;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceOrderRepository maintenanceRepository;
    private final CondominiumRepository condominiumRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final RealtimeEmitterService realtimeEmitterService;

    @Transactional(readOnly = true)
    public List<MaintenanceOrderDTO> listOrders(Long condominiumId, String status) {
        condominiumAccessService.validateAccess(condominiumId);
        List<MaintenanceOrder> orders;
        if (status != null && !status.isBlank()) {
            orders = maintenanceRepository.findAllByCondominiumIdAndStatusWithDetails(condominiumId, status.trim().toUpperCase());
        } else {
            orders = maintenanceRepository.findAllByCondominiumIdWithDetails(condominiumId);
        }
        return orders.stream().map(MaintenanceOrderDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceOrderDTO getOrderById(Long id) {
        MaintenanceOrder order = maintenanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + id));
        condominiumAccessService.validateAccess(order.getCondominium().getId());
        return new MaintenanceOrderDTO(order);
    }

    @Transactional
    public MaintenanceOrderDTO createOrder(Long condominiumId, CreateMaintenanceOrderDTO dto) {
        condominiumAccessService.validateAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        User caller = authenticatedUserService.getAuthenticatedUser();

        MaintenanceOrder order = new MaintenanceOrder();
        order.setCondominium(condo);
        order.setTitle(dto.title().trim());
        order.setDescription(dto.description().trim());
        order.setCategory(dto.category() != null ? dto.category().trim().toUpperCase() : "OUTROS");
        order.setType(dto.type() != null ? dto.type().trim().toUpperCase() : "CORRECTIVE");
        order.setPriority(dto.priority() != null ? dto.priority().trim().toUpperCase() : "MEDIUM");
        order.setStatus("OPEN");
        order.setContractorName(dto.contractorName() != null ? dto.contractorName().trim() : null);
        order.setContractorContact(dto.contractorContact() != null ? dto.contractorContact().trim() : null);
        order.setScheduledDate(dto.scheduledDate());
        order.setEstimatedCost(dto.estimatedCost());
        order.setNotes(dto.notes() != null ? dto.notes().trim() : null);
        order.setCreatedBy(caller);
        order.setCreatedAt(LocalDateTime.now());

        order = maintenanceRepository.save(order);
        MaintenanceOrderDTO result = new MaintenanceOrderDTO(order);

        realtimeEmitterService.sendToCondominium(condominiumId,
                RealtimeEventDTO.of("MAINTENANCE_UPDATED", condominiumId, result));

        return result;
    }

    @Transactional
    public MaintenanceOrderDTO updateStatus(Long id, UpdateStatusDTO dto) {
        User caller = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_MANAGER");
        if (!isStaff) {
            throw new ForbiddenException("Apenas o síndico ou administrador pode atualizar ordens de serviço.");
        }

        MaintenanceOrder order = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + id));

        String newStatus = dto.status().trim().toUpperCase();
        order.setStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) {
            order.setCompletedAt(LocalDateTime.now());
            if (dto.actualCost() != null) {
                order.setActualCost(dto.actualCost());
            }
        }
        if (dto.notes() != null && !dto.notes().isBlank()) {
            String updatedNotes = order.getNotes() != null
                    ? order.getNotes() + "\n" + dto.notes().trim()
                    : dto.notes().trim();
            order.setNotes(updatedNotes);
        }

        order = maintenanceRepository.save(order);
        MaintenanceOrderDTO result = new MaintenanceOrderDTO(order);

        realtimeEmitterService.sendToCondominium(order.getCondominium().getId(),
                RealtimeEventDTO.of("MAINTENANCE_UPDATED", order.getCondominium().getId(), result));

        return result;
    }
}
