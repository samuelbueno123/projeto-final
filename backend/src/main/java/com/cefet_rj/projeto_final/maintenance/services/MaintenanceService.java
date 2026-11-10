package com.cefet_rj.projeto_final.maintenance.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.CreateMaintenanceOrderDTO;
import com.cefet_rj.projeto_final.maintenance.dtos.MaintenanceOrderDTO.UpdateStatusDTO;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceChecklist;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceChecklistItem;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceEquipment;
import com.cefet_rj.projeto_final.maintenance.entities.MaintenanceOrder;
import com.cefet_rj.projeto_final.maintenance.entities.ServiceContract;
import com.cefet_rj.projeto_final.maintenance.entities.Supplier;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceChecklistRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceEquipmentRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceOrderRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.ServiceContractRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.SupplierRepository;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceOrderRepository maintenanceRepository;
    private final MaintenanceEquipmentRepository equipmentRepository;
    private final SupplierRepository supplierRepository;
    private final MaintenanceChecklistRepository checklistRepository;
    private final ServiceContractRepository contractRepository;
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
        condominiumAccessService.validateManagerAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        User caller = authenticatedUserService.getAuthenticatedUser();

        if (dto.title() == null || dto.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título da ordem de serviço é obrigatório.");
        }
        if (dto.estimatedCost() != null && dto.estimatedCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custo estimado não pode ser negativo.");
        }

        MaintenanceEquipment equipment = null;
        if (dto.equipmentId() != null) {
            equipment = equipmentRepository.findById(dto.equipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + dto.equipmentId()));
            if (!equipment.getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipamento não pertence a este condomínio.");
            }
        }

        Supplier supplier = null;
        if (dto.supplierId() != null) {
            supplier = supplierRepository.findById(dto.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + dto.supplierId()));
            if (!supplier.getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor não pertence a este condomínio.");
            }
        }

        MaintenanceOrder order = new MaintenanceOrder();
        order.setCondominium(condo);
        order.setEquipment(equipment);
        order.setSupplier(supplier);
        order.setTitle(dto.title().trim());
        order.setDescription(dto.description().trim());
        order.setCategory(dto.category() != null ? dto.category().trim().toUpperCase() : "OUTROS");
        order.setType(dto.type() != null ? dto.type().trim().toUpperCase() : (equipment != null ? "PREVENTIVE" : "CORRECTIVE"));
        order.setPriority(dto.priority() != null ? dto.priority().trim().toUpperCase() : "MEDIUM");
        order.setStatus("OPEN");
        order.setContractorName(dto.contractorName() != null && !dto.contractorName().isBlank()
                ? dto.contractorName().trim()
                : (supplier != null ? supplier.getCompanyName() : null));
        order.setContractorContact(dto.contractorContact() != null && !dto.contractorContact().isBlank()
                ? dto.contractorContact().trim()
                : (supplier != null ? supplier.getPhone() : null));
        order.setResponsibleName(dto.responsibleName() != null && !dto.responsibleName().isBlank()
                ? dto.responsibleName().trim()
                : (equipment != null ? equipment.getResponsibleName() : null));
        order.setRecurrenceDays(dto.recurrenceDays() != null
                ? dto.recurrenceDays()
                : (equipment != null ? equipment.getRecurrenceDays() : null));
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
        MaintenanceOrder order = maintenanceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + id));
        Long condoId = order.getCondominium().getId();
        condominiumAccessService.validateManagerAccess(condoId);

        String newStatus = dto.status().trim().toUpperCase();
        if (!List.of("OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de manutenção inválido: " + dto.status());
        }
        if ("CANCELLED".equals(order.getStatus()) && !"CANCELLED".equals(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível alterar uma ordem de serviço já cancelada.");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            if ("COMPLETED".equals(newStatus)) {
                return new MaintenanceOrderDTO(order);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível reabrir uma ordem de serviço já concluída.");
        }
        if (dto.actualCost() != null && dto.actualCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custo real não pode ser negativo.");
        }

        order.setStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) {
            order.setCompletedAt(LocalDateTime.now());
            if (dto.actualCost() != null) {
                order.setActualCost(dto.actualCost());
            }
            if (dto.supplierRating() != null && dto.supplierRating() >= 1 && dto.supplierRating() <= 5) {
                order.setSupplierRating(dto.supplierRating());
                if (dto.supplierFeedback() != null && !dto.supplierFeedback().isBlank()) {
                    order.setSupplierFeedback(dto.supplierFeedback().trim());
                }
                if (order.getSupplier() != null) {
                    applySupplierRating(order.getSupplier(), dto.supplierRating());
                }
            }
            if (order.getEquipment() != null) {
                MaintenanceEquipment eq = order.getEquipment();
                LocalDate today = LocalDate.now();
                eq.setLastMaintenanceDate(today);
                int rec = eq.getRecurrenceDays() != null && eq.getRecurrenceDays() > 0 ? eq.getRecurrenceDays() : 90;
                eq.setNextMaintenanceDate(today.plusDays(rec));
                eq.setStatus("OPERATIONAL");
                equipmentRepository.save(eq);
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

        realtimeEmitterService.sendToCondominium(condoId,
                RealtimeEventDTO.of("MAINTENANCE_UPDATED", condoId, result));

        return result;
    }

    // =========================================================================
    // FEATURE 3: PREVENTIVE MAINTENANCE EQUIPMENT & SCHEDULES
    // =========================================================================

    @Transactional(readOnly = true)
    public List<MaintenanceEquipmentDTO> listEquipment(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        return equipmentRepository.findByCondominiumIdOrderByNextMaintenanceDateAsc(condominiumId).stream()
                .map(eq -> new MaintenanceEquipmentDTO(
                        eq,
                        maintenanceRepository.findByEquipmentIdOrderByCreatedAtDesc(eq.getId()).stream()
                                .map(MaintenanceOrderDTO::new)
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public MaintenanceEquipmentDTO createEquipment(Long condominiumId, Map<String, Object> body) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        String name = stringVal(body, "name");
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do equipamento ou estrutura.");
        }

        int recurrenceDays = intVal(body.get("recurrenceDays"), 90);
        if (recurrenceDays <= 0) recurrenceDays = 90;

        LocalDate lastDate = parseLocalDate(stringVal(body, "lastMaintenanceDate"), null);
        LocalDate defaultNext = lastDate != null ? lastDate.plusDays(recurrenceDays) : LocalDate.now().plusDays(recurrenceDays);
        LocalDate nextDate = parseLocalDate(stringVal(body, "nextMaintenanceDate"), defaultNext);

        Supplier preferredSupplier = null;
        Long supplierId = longVal(body.get("preferredSupplierId"));
        if (supplierId != null) {
            preferredSupplier = supplierRepository.findById(supplierId)
                    .filter(s -> s.getCondominium().getId().equals(condominiumId))
                    .orElse(null);
        }

        MaintenanceEquipment eq = new MaintenanceEquipment();
        eq.setCondominium(condo);
        eq.setName(name);
        eq.setCategory(defaultIfBlank(stringVal(body, "category"), "ELEVADORES").toUpperCase(Locale.ROOT));
        eq.setLocation(blankToNull(stringVal(body, "location")));
        eq.setManufacturer(blankToNull(stringVal(body, "manufacturer")));
        eq.setModelNumber(blankToNull(stringVal(body, "modelNumber")));
        eq.setSerialNumber(blankToNull(stringVal(body, "serialNumber")));
        eq.setInstalledAt(parseLocalDate(stringVal(body, "installedAt"), null));
        eq.setRecurrenceDays(recurrenceDays);
        eq.setLastMaintenanceDate(lastDate);
        eq.setNextMaintenanceDate(nextDate);
        eq.setResponsibleName(blankToNull(stringVal(body, "responsibleName")));
        eq.setPreferredSupplier(preferredSupplier);
        eq.setStatus(defaultIfBlank(stringVal(body, "status"), "OPERATIONAL").toUpperCase(Locale.ROOT));
        eq.setNotes(blankToNull(stringVal(body, "notes")));
        eq.setCreatedAt(LocalDateTime.now());
        eq = equipmentRepository.save(eq);

        return new MaintenanceEquipmentDTO(eq, List.of());
    }

    @Transactional
    public MaintenanceEquipmentDTO updateEquipment(Long id, Map<String, Object> body) {
        MaintenanceEquipment eq = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
        Long condoId = eq.getCondominium().getId();
        condominiumAccessService.validateManagerAccess(condoId);

        if (!stringVal(body, "name").isBlank()) {
            eq.setName(stringVal(body, "name"));
        }
        if (!stringVal(body, "category").isBlank()) {
            eq.setCategory(stringVal(body, "category").toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("location")) {
            eq.setLocation(blankToNull(stringVal(body, "location")));
        }
        if (body.containsKey("manufacturer")) {
            eq.setManufacturer(blankToNull(stringVal(body, "manufacturer")));
        }
        if (body.containsKey("modelNumber")) {
            eq.setModelNumber(blankToNull(stringVal(body, "modelNumber")));
        }
        if (body.containsKey("serialNumber")) {
            eq.setSerialNumber(blankToNull(stringVal(body, "serialNumber")));
        }
        if (body.containsKey("recurrenceDays")) {
            int rec = intVal(body.get("recurrenceDays"), eq.getRecurrenceDays());
            if (rec > 0) eq.setRecurrenceDays(rec);
        }
        if (body.containsKey("lastMaintenanceDate")) {
            eq.setLastMaintenanceDate(parseLocalDate(stringVal(body, "lastMaintenanceDate"), eq.getLastMaintenanceDate()));
        }
        if (body.containsKey("nextMaintenanceDate")) {
            eq.setNextMaintenanceDate(parseLocalDate(stringVal(body, "nextMaintenanceDate"), eq.getNextMaintenanceDate()));
        }
        if (body.containsKey("responsibleName")) {
            eq.setResponsibleName(blankToNull(stringVal(body, "responsibleName")));
        }
        if (body.containsKey("preferredSupplierId")) {
            Long supId = longVal(body.get("preferredSupplierId"));
            eq.setPreferredSupplier(supId != null
                    ? supplierRepository.findById(supId).filter(s -> s.getCondominium().getId().equals(condoId)).orElse(null)
                    : null);
        }
        if (!stringVal(body, "status").isBlank()) {
            eq.setStatus(stringVal(body, "status").toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("notes")) {
            eq.setNotes(blankToNull(stringVal(body, "notes")));
        }
        eq = equipmentRepository.save(eq);
        return new MaintenanceEquipmentDTO(
                eq,
                maintenanceRepository.findByEquipmentIdOrderByCreatedAtDesc(eq.getId()).stream()
                        .map(MaintenanceOrderDTO::new)
                        .toList()
        );
    }

    @Transactional
    public void deleteEquipment(Long id) {
        MaintenanceEquipment eq = equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
        condominiumAccessService.validateManagerAccess(eq.getCondominium().getId());
        equipmentRepository.delete(eq);
    }

    // =========================================================================
    // FEATURE 4: SUPPLIERS & SERVICE PROVIDERS MANAGEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SupplierDTO> listSuppliers(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        return supplierRepository.findByCondominiumIdOrderByCompanyNameAsc(condominiumId).stream()
                .map(s -> new SupplierDTO(
                        s,
                        maintenanceRepository.findBySupplierIdOrderByCreatedAtDesc(s.getId()).stream()
                                .map(MaintenanceOrderDTO::new)
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public SupplierDTO createSupplier(Long condominiumId, Map<String, Object> body) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        String companyName = defaultIfBlank(stringVal(body, "companyName"), stringVal(body, "name"));
        if (companyName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome da empresa ou prestador.");
        }

        Supplier s = new Supplier();
        s.setCondominium(condo);
        s.setCompanyName(companyName);
        s.setContactName(blankToNull(stringVal(body, "contactName")));
        s.setPhone(blankToNull(stringVal(body, "phone")));
        s.setEmail(blankToNull(stringVal(body, "email")));
        s.setDocumentNumber(blankToNull(stringVal(body, "documentNumber")));
        String rawCat = defaultIfBlank(stringVal(body, "serviceCategory"), stringVal(body, "category"));
        s.setServiceCategory(defaultIfBlank(rawCat, "GERAL").toUpperCase(Locale.ROOT));
        s.setAddress(blankToNull(stringVal(body, "address")));
        s.setNotes(blankToNull(stringVal(body, "notes")));
        s.setRatingAverage(BigDecimal.ZERO);
        s.setRatingCount(0);
        s.setActive(body.get("active") == null || Boolean.TRUE.equals(body.get("active")));
        s.setCreatedAt(LocalDateTime.now());
        s = supplierRepository.save(s);

        return new SupplierDTO(s, List.of());
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, Map<String, Object> body) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + id));
        condominiumAccessService.validateManagerAccess(s.getCondominium().getId());

        if (!stringVal(body, "companyName").isBlank()) {
            s.setCompanyName(stringVal(body, "companyName"));
        }
        if (body.containsKey("contactName")) {
            s.setContactName(blankToNull(stringVal(body, "contactName")));
        }
        if (body.containsKey("phone")) {
            s.setPhone(blankToNull(stringVal(body, "phone")));
        }
        if (body.containsKey("email")) {
            s.setEmail(blankToNull(stringVal(body, "email")));
        }
        if (body.containsKey("documentNumber")) {
            s.setDocumentNumber(blankToNull(stringVal(body, "documentNumber")));
        }
        if (!stringVal(body, "serviceCategory").isBlank()) {
            s.setServiceCategory(stringVal(body, "serviceCategory").toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("address")) {
            s.setAddress(blankToNull(stringVal(body, "address")));
        }
        if (body.containsKey("notes")) {
            s.setNotes(blankToNull(stringVal(body, "notes")));
        }
        if (body.containsKey("active") && body.get("active") != null) {
            s.setActive(Boolean.TRUE.equals(body.get("active")));
        }
        if (body.containsKey("rating")) {
            int rating = intVal(body.get("rating"), 0);
            if (rating >= 1 && rating <= 5) {
                applySupplierRating(s, rating);
            }
        }
        s = supplierRepository.save(s);
        return new SupplierDTO(
                s,
                maintenanceRepository.findBySupplierIdOrderByCreatedAtDesc(s.getId()).stream()
                        .map(MaintenanceOrderDTO::new)
                        .toList()
        );
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + id));
        condominiumAccessService.validateManagerAccess(s.getCondominium().getId());
        supplierRepository.delete(s);
    }

    private void applySupplierRating(Supplier supplier, int rating) {
        int currentCount = supplier.getRatingCount() != null ? supplier.getRatingCount() : 0;
        BigDecimal currentAvg = supplier.getRatingAverage() != null ? supplier.getRatingAverage() : BigDecimal.ZERO;
        BigDecimal totalScore = currentAvg.multiply(BigDecimal.valueOf(currentCount)).add(BigDecimal.valueOf(rating));
        int newCount = currentCount + 1;
        supplier.setRatingCount(newCount);
        supplier.setRatingAverage(totalScore.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP));
        supplierRepository.save(supplier);
    }

    private static String stringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key);
        return val == null ? "" : val.toString().trim();
    }

    private static String blankToNull(String val) {
        return val == null || val.isBlank() ? null : val.trim();
    }

    private static String defaultIfBlank(String val, String fallback) {
        return val == null || val.isBlank() ? fallback : val.trim();
    }

    private static int intVal(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        if (raw != null && !raw.toString().isBlank()) {
            try {
                return Integer.parseInt(raw.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private static Long longVal(Object raw) {
        if (raw instanceof Number n) return n.longValue();
        if (raw != null && !raw.toString().isBlank()) {
            try {
                return Long.parseLong(raw.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static LocalDate parseLocalDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDate.parse(raw.trim().substring(0, 10));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record MaintenanceEquipmentDTO(
            Long id,
            Long condominiumId,
            String name,
            String category,
            String location,
            String manufacturer,
            String modelNumber,
            String serialNumber,
            LocalDate installedAt,
            Integer recurrenceDays,
            LocalDate lastMaintenanceDate,
            LocalDate nextMaintenanceDate,
            long daysUntilNextMaintenance,
            String alertStatus,
            boolean overdue,
            boolean dueSoon,
            String responsibleName,
            Long preferredSupplierId,
            String preferredSupplierName,
            String status,
            String notes,
            LocalDateTime createdAt,
            List<MaintenanceOrderDTO> interventionHistory
    ) {
        public MaintenanceEquipmentDTO(MaintenanceEquipment eq, List<MaintenanceOrderDTO> history) {
            this(
                    eq.getId(),
                    eq.getCondominium().getId(),
                    eq.getName(),
                    eq.getCategory(),
                    eq.getLocation(),
                    eq.getManufacturer(),
                    eq.getModelNumber(),
                    eq.getSerialNumber(),
                    eq.getInstalledAt(),
                    eq.getRecurrenceDays(),
                    eq.getLastMaintenanceDate(),
                    eq.getNextMaintenanceDate(),
                    eq.getNextMaintenanceDate() != null ? ChronoUnit.DAYS.between(LocalDate.now(), eq.getNextMaintenanceDate()) : 0,
                    computeAlertStatus(eq.getNextMaintenanceDate()),
                    "OVERDUE".equals(computeAlertStatus(eq.getNextMaintenanceDate())),
                    "DUE_SOON".equals(computeAlertStatus(eq.getNextMaintenanceDate())),
                    eq.getResponsibleName(),
                    eq.getPreferredSupplier() != null ? eq.getPreferredSupplier().getId() : null,
                    eq.getPreferredSupplier() != null ? eq.getPreferredSupplier().getCompanyName() : null,
                    eq.getStatus(),
                    eq.getNotes(),
                    eq.getCreatedAt(),
                    history
            );
        }

        private static String computeAlertStatus(LocalDate nextDate) {
            if (nextDate == null) return "OK";
            LocalDate today = LocalDate.now();
            if (nextDate.isBefore(today)) return "OVERDUE";
            if (!nextDate.isAfter(today.plusDays(15))) return "DUE_SOON";
            return "OK";
        }
    }

    public record SupplierDTO(
            Long id,
            Long condominiumId,
            String companyName,
            String contactName,
            String phone,
            String email,
            String documentNumber,
            String serviceCategory,
            String address,
            String notes,
            BigDecimal ratingAverage,
            Integer ratingCount,
            Boolean active,
            LocalDateTime createdAt,
            List<MaintenanceOrderDTO> serviceHistory
    ) {
        public SupplierDTO(Supplier s, List<MaintenanceOrderDTO> history) {
            this(
                    s.getId(),
                    s.getCondominium().getId(),
                    s.getCompanyName(),
                    s.getContactName(),
                    s.getPhone(),
                    s.getEmail(),
                    s.getDocumentNumber(),
                    s.getServiceCategory(),
                    s.getAddress(),
                    s.getNotes(),
                    s.getRatingAverage(),
                    s.getRatingCount(),
                    s.getActive(),
                    s.getCreatedAt(),
                    history
            );
        }
    }

    // =========================================================================
    // INSPEÇÃO PREDIAL PERIÓDICA / CHECKLISTS DE MANUTENÇÃO PREVENTIVA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<MaintenanceChecklistDTO> listChecklists(Long condominiumId) {
        condominiumAccessService.validateStaffAccess(condominiumId);
        return checklistRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream().map(MaintenanceChecklistDTO::new).toList();
    }

    @Transactional
    public MaintenanceChecklistDTO createChecklist(Long condominiumId, Map<String, Object> body) {
        User staff = authenticatedUserService.getAuthenticatedUser();
        condominiumAccessService.validateStaffAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        String title = body.get("title") != null ? body.get("title").toString().trim() : "Inspeção Preventiva";
        String freq = body.get("frequency") != null ? body.get("frequency").toString().trim().toUpperCase(Locale.ROOT) : "MONTHLY";

        MaintenanceChecklist cl = new MaintenanceChecklist();
        cl.setCondominium(condo);
        cl.setTitle(title);
        cl.setFrequency(freq);
        cl.setStatus("DRAFT");
        cl.setInspector(staff);
        cl.setGeneralNotes(body.get("generalNotes") != null ? body.get("generalNotes").toString().trim() : null);
        cl.setCreatedAt(LocalDateTime.now());

        if (body.get("items") instanceof List<?> rawList) {
            for (Object o : rawList) {
                if (o instanceof Map<?, ?> itemMap) {
                    MaintenanceChecklistItem item = new MaintenanceChecklistItem();
                    item.setChecklist(cl);
                    item.setItemDescription(itemMap.get("itemDescription") != null ? itemMap.get("itemDescription").toString().trim() : "Item de verificação");
                    item.setAreaOrLocation(itemMap.get("areaOrLocation") != null ? itemMap.get("areaOrLocation").toString().trim() : null);
                    item.setStatus(itemMap.get("status") != null ? itemMap.get("status").toString().trim().toUpperCase(Locale.ROOT) : "CONFORMING");
                    item.setObservation(itemMap.get("observation") != null ? itemMap.get("observation").toString().trim() : null);
                    if (itemMap.get("equipmentId") instanceof Number eqId) {
                        equipmentRepository.findById(eqId.longValue()).ifPresent(item::setEquipment);
                    }
                    cl.getItems().add(item);
                }
            }
        }

        return new MaintenanceChecklistDTO(checklistRepository.save(cl));
    }

    @Transactional
    public MaintenanceChecklistDTO completeChecklist(Long id, Map<String, Object> body) {
        MaintenanceChecklist cl = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist não encontrado: " + id));
        condominiumAccessService.validateStaffAccess(cl.getCondominium().getId());

        cl.setStatus("COMPLETED");
        cl.setInspectedAt(LocalDateTime.now());
        if (body != null && body.containsKey("generalNotes")) {
            cl.setGeneralNotes(body.get("generalNotes") != null ? body.get("generalNotes").toString().trim() : null);
        }

        // Se houver algum item não conforme, marca como REQUIRES_ACTION
        boolean hasIrregular = cl.getItems().stream().anyMatch(i -> "NON_CONFORMING".equalsIgnoreCase(i.getStatus()));
        if (hasIrregular) {
            cl.setStatus("REQUIRES_ACTION");
        }

        return new MaintenanceChecklistDTO(checklistRepository.save(cl));
    }

    // =========================================================================
    // GESTÃO DE CONTRATOS RECORRENTES E GARANTIAS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ServiceContractDTO> listContracts(Long condominiumId, String status) {
        condominiumAccessService.validateStaffAccess(condominiumId);
        List<ServiceContract> list = (status != null && !status.isBlank())
                ? contractRepository.findByCondominiumIdAndStatusOrderByEndDateAsc(condominiumId, status.trim().toUpperCase(Locale.ROOT))
                : contractRepository.findByCondominiumIdOrderByEndDateAsc(condominiumId);
        return list.stream().map(ServiceContractDTO::new).toList();
    }

    @Transactional
    public ServiceContractDTO createContract(Long condominiumId, Map<String, Object> body) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        String title = body.get("title") != null ? body.get("title").toString().trim() : "";
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título do contrato é obrigatório.");
        }

        Supplier supplier = null;
        if (body.get("supplierId") instanceof Number sid) {
            supplier = supplierRepository.findById(sid.longValue()).orElse(null);
        }

        BigDecimal val = BigDecimal.ZERO;
        if (body.get("monthlyValue") instanceof Number n) val = BigDecimal.valueOf(n.doubleValue());
        else if (body.get("monthlyValue") != null) {
            try { val = new BigDecimal(body.get("monthlyValue").toString().trim()); } catch (Exception ignored) {}
        }

        LocalDate start = LocalDate.now();
        if (body.get("startDate") != null) {
            try { start = LocalDate.parse(body.get("startDate").toString().trim().substring(0, 10)); } catch (Exception ignored) {}
        }
        LocalDate end = start.plusYears(1);
        if (body.get("endDate") != null) {
            try { end = LocalDate.parse(body.get("endDate").toString().trim().substring(0, 10)); } catch (Exception ignored) {}
        }

        ServiceContract sc = new ServiceContract();
        sc.setCondominium(condo);
        sc.setSupplier(supplier);
        sc.setTitle(title);
        sc.setContractNumber(body.get("contractNumber") != null ? body.get("contractNumber").toString().trim() : null);
        sc.setCategory(body.get("category") != null ? body.get("category").toString().trim().toUpperCase(Locale.ROOT) : "MANUTENCAO");
        sc.setMonthlyValue(val);
        sc.setStartDate(start);
        sc.setEndDate(end);
        sc.setAutoRenew(Boolean.TRUE.equals(body.get("autoRenew")));
        sc.setRenewalNoticeDays(body.get("renewalNoticeDays") instanceof Number n ? n.intValue() : 30);
        sc.setStatus("ACTIVE");
        sc.setCoverageDescription(body.get("coverageDescription") != null ? body.get("coverageDescription").toString().trim() : null);
        sc.setDocumentUrl(body.get("documentUrl") != null ? body.get("documentUrl").toString().trim() : null);
        sc.setCreatedAt(LocalDateTime.now());

        return new ServiceContractDTO(contractRepository.save(sc));
    }

    @Transactional
    public ServiceContractDTO terminateContract(Long id) {
        ServiceContract sc = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + id));
        condominiumAccessService.validateManagerAccess(sc.getCondominium().getId());
        sc.setStatus("TERMINATED");
        return new ServiceContractDTO(contractRepository.save(sc));
    }

    public record MaintenanceChecklistItemDTO(
            Long id,
            String itemDescription,
            String areaOrLocation,
            String status,
            String observation,
            Long equipmentId,
            String equipmentName
    ) {
        public MaintenanceChecklistItemDTO(MaintenanceChecklistItem i) {
            this(
                    i.getId(),
                    i.getItemDescription(),
                    i.getAreaOrLocation(),
                    i.getStatus(),
                    i.getObservation(),
                    i.getEquipment() != null ? i.getEquipment().getId() : null,
                    i.getEquipment() != null ? i.getEquipment().getName() : null
            );
        }
    }

    public record MaintenanceChecklistDTO(
            Long id,
            Long condominiumId,
            String title,
            String frequency,
            String status,
            LocalDateTime inspectedAt,
            String inspectorName,
            String generalNotes,
            LocalDateTime createdAt,
            List<MaintenanceChecklistItemDTO> items
    ) {
        public MaintenanceChecklistDTO(MaintenanceChecklist c) {
            this(
                    c.getId(),
                    c.getCondominium().getId(),
                    c.getTitle(),
                    c.getFrequency(),
                    c.getStatus(),
                    c.getInspectedAt(),
                    c.getInspector() != null ? (c.getInspector().getFirstName() + " " + c.getInspector().getLastName()).trim() : null,
                    c.getGeneralNotes(),
                    c.getCreatedAt(),
                    c.getItems() != null ? c.getItems().stream().map(MaintenanceChecklistItemDTO::new).toList() : List.of()
            );
        }
    }

    public record ServiceContractDTO(
            Long id,
            Long condominiumId,
            Long supplierId,
            String supplierName,
            String title,
            String contractNumber,
            String category,
            BigDecimal monthlyValue,
            LocalDate startDate,
            LocalDate endDate,
            Boolean autoRenew,
            Integer renewalNoticeDays,
            String status,
            String coverageDescription,
            String documentUrl,
            LocalDateTime createdAt
    ) {
        public ServiceContractDTO(ServiceContract c) {
            this(
                    c.getId(),
                    c.getCondominium().getId(),
                    c.getSupplier() != null ? c.getSupplier().getId() : null,
                    c.getSupplier() != null ? c.getSupplier().getCompanyName() : null,
                    c.getTitle(),
                    c.getContractNumber(),
                    c.getCategory(),
                    c.getMonthlyValue(),
                    c.getStartDate(),
                    c.getEndDate(),
                    c.getAutoRenew(),
                    c.getRenewalNoticeDays(),
                    c.getStatus(),
                    c.getCoverageDescription(),
                    c.getDocumentUrl(),
                    c.getCreatedAt()
            );
        }
    }
}
