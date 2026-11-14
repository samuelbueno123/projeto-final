package com.cefet_rj.projeto_final.manager.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.SectorDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.UnitDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.manager.dtos.ManagerDashboardDTO;
import com.cefet_rj.projeto_final.manager.dtos.UnitPaymentUpdateDTO;
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ManagerDashboardController {
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumRepository condominiumRepository;
    private final MembershipRepository membershipRepository;
    private final SectorRepository sectorRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final UnitPaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final CondominiumAccessService condominiumAccessService;
    private final NotificationService notificationService;
    private final BillingPlanRepository billingPlanRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService featureAccessService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService realtimeEmitterService;

    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public ManagerDashboardDTO dashboard(@RequestParam Long condominiumId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId).orElseThrow();
        var rawUnits = unitRepository.findAllForCondominium(condominiumId);
        List<Long> unitIds = rawUnits.stream().map(Unit::getId).toList();
        Map<Long, UnitPayment> paymentMap = unitIds.isEmpty()
                ? Map.of()
                : paymentRepository.findAllByUnitIdIn(unitIds).stream()
                .collect(Collectors.toMap(p -> p.getUnit().getId(), p -> p, (a, b) -> a));

        var units = rawUnits.stream().map(unit -> {
            var payment = paymentMap.get(unit.getId());
            return new ManagerDashboardDTO.UnitInfo(
                    unit.getId(),
                    unit.getIdentifier(),
                    unit.getSector().getName(),
                    payment == null ? "NOT_RECORDED" : payment.getStatus().name(),
                    payment == null ? null : payment.getDueDate(),
                    payment == null ? null : payment.getAmount(),
                    payment == null ? null : payment.getPaymentNote(),
                    payment == null ? null : payment.getRequestedAt(),
                    payment == null ? null : payment.getInvoiceTitle(),
                    payment == null ? null : payment.getDigitableLine(),
                    payment == null ? null : payment.getPaidAt()
            );
        }).toList();
        var reservations = reservationRepository.findByCommonAreaCondominiumIdOrderByStartAtDesc(condominiumId)
                .stream().map(r -> new ManagerDashboardDTO.ReservationInfo(
                        r.getId(),
                        r.getStartAt(),
                        r.getEndAt(),
                        r.getStatus().name(),
                        r.getCommonArea().getId(),
                        r.getCommonArea().getName(),
                        r.getReservedBy().getFirstName() + " " + r.getReservedBy().getLastName(),
                        r.getReservedBy().getEmail(),
                        r.getContactPhone(),
                        r.getNotes(),
                        r.getRejectionReason(),
                        r.getCounterProposalNotes(),
                        r.getProposedStartAt(),
                        r.getProposedEndAt()
                ))
                .toList();
        return new ManagerDashboardDTO(
                condominiumId,
                condo.getName(),
                units.size(),
                membershipRepository.countDistinctUsers(condominiumId),
                reservations.size(),
                units,
                reservations
        );
    }

    @PutMapping("/manager/condominiums/{condominiumId}/select-plan")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ResponseEntity<CondominiumDTO> selectCondominiumPlan(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado"));
        Long planId = body.get("planId") instanceof Number n ? n.longValue() : null;
        if (planId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o plano desejado.");
        }
        BillingPlan plan = billingPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + planId));
        condo.setBillingPlan(plan);
        condo.setPlanSelected(true);
        condo = condominiumRepository.save(condo);
        if (featureAccessService != null) {
            featureAccessService.evictCondoFeaturesCache();
        }
        return ResponseEntity.ok(new CondominiumDTO(condo));
    }

    @PutMapping("/manager/condominiums/{condominiumId}/units/{unitId}/payment-status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ResponseEntity<Void> updatePayment(
            @PathVariable Long condominiumId,
            @PathVariable Long unitId,
            @Valid @RequestBody UnitPaymentUpdateDTO dto
    ) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Unit unit = unitRepository.findById(unitId).orElseThrow();
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new AccessDeniedException("Unidade fora deste condomínio");
        }
        UnitPayment payment = paymentRepository.findByUnitId(unitId).orElseGet(UnitPayment::new);
        payment.setUnit(unit);
        payment.setStatus(dto.status());
        payment.setDueDate(dto.dueDate());
        payment.setAmount(dto.amount());
        payment.setPaymentNote(dto.paymentNote());
        if (payment.getStatus() == UnitPaymentStatus.PAID) {
            if (payment.getPaidAt() == null) {
                payment.setPaidAt(LocalDateTime.now());
            }
        } else {
            payment.setPaidAt(null);
        }
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(viewer);
        payment = paymentRepository.save(payment);
        if (realtimeEmitterService != null) {
            realtimeEmitterService.sendToCondominium(
                    condominiumId,
                    com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO.of(
                            "INVOICE_UPDATED",
                            condominiumId,
                            toUnitInfo(unit, payment)
                    )
            );
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/manager/condominiums/{condominiumId}/units/{unitId}/generate-boleto")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ResponseEntity<ManagerDashboardDTO.UnitInfo> generateBoleto(
            @PathVariable Long condominiumId,
            @PathVariable Long unitId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new AccessDeniedException("Unidade fora deste condomínio");
        }
        BigDecimal amount = body != null && body.get("amount") != null
                ? new BigDecimal(body.get("amount").toString())
                : new BigDecimal("450.00");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O valor do boleto deve ser maior que zero.");
        }
        LocalDate dueDate = body != null && body.get("dueDate") != null
                ? LocalDate.parse(body.get("dueDate").toString())
                : LocalDate.now().plusDays(10);
        String title = body != null && body.get("title") != null && !body.get("title").toString().isBlank()
                ? body.get("title").toString().trim()
                : "Taxa de Condomínio - " + LocalDate.now().getMonth().name().substring(0, 3) + "/" + LocalDate.now().getYear();

        String digitableLine = "34191.79001 01043.510047 91020.150008 8 " + String.format("%014d", amount.multiply(new BigDecimal(100)).longValue());

        UnitPayment payment = paymentRepository.findByUnitId(unitId).orElseGet(UnitPayment::new);
        payment.setUnit(unit);
        payment.setStatus(UnitPaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setDueDate(dueDate);
        payment.setInvoiceTitle(title);
        payment.setDigitableLine(digitableLine);
        payment.setPaidAt(null);
        payment.setPaymentNote("Boleto gerado pelo síndico " + viewer.getFirstName());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(viewer);
        payment = paymentRepository.save(payment);
        ManagerDashboardDTO.UnitInfo unitInfo = toUnitInfo(unit, payment);
        if (realtimeEmitterService != null) {
            realtimeEmitterService.sendToCondominium(
                    condominiumId,
                    com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO.of(
                            "INVOICE_UPDATED",
                            condominiumId,
                            unitInfo
                    )
            );
        }
        return ResponseEntity.ok(unitInfo);
    }

    @PostMapping("/condominiums/{condominiumId}/units/{unitId}/pay-boleto")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ManagerDashboardDTO.UnitInfo> payBoleto(
            @PathVariable Long condominiumId,
            @PathVariable Long unitId
    ) {
        User user = authenticatedUserService.getAuthenticatedUser();
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new AccessDeniedException("Unidade fora deste condomínio");
        }
        ensureUnitOrManagerAccess(user, condominiumId, unitId);
        UnitPayment payment = paymentRepository.findByUnitId(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Cobrança não encontrada para esta unidade"));

        if (payment.getStatus() == UnitPaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta cobrança já foi liquidada anteriormente.");
        }

        payment.setStatus(UnitPaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentNote("Pagamento liquidado via aplicativo por " + user.getFirstName());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(user);
        payment = paymentRepository.save(payment);
        ManagerDashboardDTO.UnitInfo unitInfo = toUnitInfo(unit, payment);
        if (realtimeEmitterService != null) {
            realtimeEmitterService.sendToCondominium(
                    condominiumId,
                    com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO.of(
                            "INVOICE_UPDATED",
                            condominiumId,
                            unitInfo
                    )
            );
        }
        return ResponseEntity.ok(unitInfo);
    }

    @GetMapping("/condominiums/{condominiumId}/units/{unitId}/payment-status")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<ManagerDashboardDTO.UnitInfo> getUnitPaymentStatus(
            @PathVariable Long condominiumId,
            @PathVariable Long unitId
    ) {
        User user = authenticatedUserService.getAuthenticatedUser();
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new AccessDeniedException("Unidade fora deste condomínio");
        }
        ensureUnitOrManagerAccess(user, condominiumId, unitId);
        UnitPayment payment = paymentRepository.findByUnitId(unitId).orElse(null);
        if (payment == null) {
            return ResponseEntity.ok(new ManagerDashboardDTO.UnitInfo(
                    unit.getId(), unit.getIdentifier(), unit.getSector().getName(),
                    "UP_TO_DATE", null, null, null, null, null, null, null
            ));
        }
        return ResponseEntity.ok(new ManagerDashboardDTO.UnitInfo(
                unit.getId(),
                unit.getIdentifier(),
                unit.getSector().getName(),
                payment.getStatus().name(),
                payment.getDueDate(),
                payment.getAmount(),
                payment.getPaymentNote(),
                payment.getRequestedAt(),
                payment.getInvoiceTitle(),
                payment.getDigitableLine(),
                payment.getPaidAt()
        ));
    }

    @GetMapping({"/condominiums/{condominiumId}/my-invoices", "/condominiums/{condominiumId}/units/my-payments"})
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ManagerDashboardDTO.UnitInfo>> getMyInvoices(
            @PathVariable Long condominiumId
    ) {
        User user = authenticatedUserService.getAuthenticatedUser();
        var residents = residentRepository.findByUserIdAndCondominiumId(user.getId(), condominiumId);
        List<Long> unitIds = residents.stream().map(r -> r.getUnit().getId()).toList();
        Map<Long, UnitPayment> paymentMap = unitIds.isEmpty()
                ? Map.of()
                : paymentRepository.findAllByUnitIdIn(unitIds).stream()
                .collect(Collectors.toMap(p -> p.getUnit().getId(), p -> p, (a, b) -> a));

        List<ManagerDashboardDTO.UnitInfo> list = residents.stream().map(r -> {
            Unit unit = r.getUnit();
            UnitPayment payment = paymentMap.get(unit.getId());
            return new ManagerDashboardDTO.UnitInfo(
                    unit.getId(),
                    unit.getIdentifier(),
                    unit.getSector().getName(),
                    payment == null ? "UP_TO_DATE" : payment.getStatus().name(),
                    payment == null ? null : payment.getDueDate(),
                    payment == null ? null : payment.getAmount(),
                    payment == null ? null : payment.getPaymentNote(),
                    payment == null ? null : payment.getRequestedAt(),
                    payment == null ? null : payment.getInvoiceTitle(),
                    payment == null ? null : payment.getDigitableLine(),
                    payment == null ? null : payment.getPaidAt()
            );
        }).toList();

        return ResponseEntity.ok(list);
    }

    @PostMapping("/condominiums/{condominiumId}/units/{unitId}/request-payment-confirmation")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ManagerDashboardDTO.UnitInfo> requestPaymentConfirmation(
            @PathVariable Long condominiumId,
            @PathVariable Long unitId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!user.hasRole("ROLE_ADMIN")) {
            condominiumAccessService.validateUserBelongsToCondominium(user.getId(), condominiumId);
        }
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new AccessDeniedException("Unidade fora deste condomínio");
        }
        ensureUnitOrManagerAccess(user, condominiumId, unitId);

        String note = body != null && body.get("note") != null && !body.get("note").isBlank()
                ? body.get("note").trim()
                : "Pagamento informado pelo morador " + user.getFirstName();

        UnitPayment payment = paymentRepository.findByUnitId(unitId).orElseGet(UnitPayment::new);
        payment.setUnit(unit);
        payment.setStatus(UnitPaymentStatus.CONFIRMATION_REQUESTED);
        payment.setPaymentNote(note);
        payment.setRequestedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(user);
        payment = paymentRepository.save(payment);

        // Notify managers
        var managers = membershipRepository.findActiveManagersByCondominiumId(condominiumId);
        for (var mgr : managers) {
            notificationService.createDirectNotification(
                    mgr.getUser(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Solicitação de baixa de condomínio",
                    user.getFirstName() + " informou o pagamento da unidade "
                            + unit.getSector().getName() + " • " + unit.getIdentifier()
                            + " (" + note + "). Confirme no painel do síndico.",
                    condominiumId,
                    unit.getSector().getCondominium().getName()
            );
        }

        ManagerDashboardDTO.UnitInfo unitInfo = toUnitInfo(unit, payment);
        if (realtimeEmitterService != null) {
            realtimeEmitterService.sendToCondominium(
                    condominiumId,
                    com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO.of(
                            "INVOICE_UPDATED",
                            condominiumId,
                            unitInfo
                    )
            );
        }

        return ResponseEntity.ok(unitInfo);
    }

    @GetMapping("/manager/condominiums/{condominiumId}/sectors")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SectorDTO>> listSectors(@PathVariable Long condominiumId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        var list = sectorRepository.findByCondominiumId(condominiumId).stream()
                .map(SectorDTO::new)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/manager/condominiums/{condominiumId}/sectors")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ResponseEntity<SectorDTO> createSector(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, String> body
    ) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado"));
        String name = body.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do setor/bloco.");
        }
        if (sectorRepository.findByCondominiumIdAndNameIgnoreCase(condominiumId, name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um setor/bloco com esse nome neste condomínio.");
        }
        SectorStructuralType type = SectorStructuralType.BLOCK;
        String rawType = body.get("structuralType");
        if (rawType != null && !rawType.isBlank()) {
            try {
                type = SectorStructuralType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Sector sector = new Sector();
        sector.setName(name);
        sector.setStructuralType(type);
        sector.setCondominium(condo);
        sector = sectorRepository.save(sector);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SectorDTO(sector));
    }

    @PostMapping("/manager/condominiums/{condominiumId}/units")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ResponseEntity<UnitDTO> createUnit(
            @PathVariable Long condominiumId,
            @RequestBody Map<String, Object> body
    ) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        String identifier = body.get("identifier") != null ? body.get("identifier").toString().trim() : "";
        Long sectorId = body.get("sectorId") instanceof Number num ? num.longValue() : null;
        if (identifier.isBlank() || sectorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o identificador e o setor da unidade.");
        }
        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));
        if (!sector.getCondominium().getId().equals(condominiumId)) {
            throw new AccessDeniedException("Setor fora deste condomínio");
        }
        if (unitRepository.existsBySectorIdAndIdentifierIgnoreCase(sector.getId(), identifier)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma unidade com este identificador neste setor.");
        }
        Unit unit = new Unit();
        unit.setIdentifier(identifier);
        unit.setSector(sector);
        unit = unitRepository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UnitDTO(unit));
    }

    private static ManagerDashboardDTO.UnitInfo toUnitInfo(Unit unit, UnitPayment payment) {
        return new ManagerDashboardDTO.UnitInfo(
                unit.getId(),
                unit.getIdentifier(),
                unit.getSector().getName(),
                payment.getStatus().name(),
                payment.getDueDate(),
                payment.getAmount(),
                payment.getPaymentNote(),
                payment.getRequestedAt(),
                payment.getInvoiceTitle(),
                payment.getDigitableLine(),
                payment.getPaidAt()
        );
    }

    private void ensureAccess(User viewer, Long condominiumId) {
        if (!viewer.hasRole("ROLE_ADMIN") && !membershipRepository.isManagerOf(viewer.getId(), condominiumId))
            throw new AccessDeniedException("Você não administra este condomínio");
    }

    private void ensureUnitOrManagerAccess(User viewer, Long condominiumId, Long unitId) {
        if (viewer.hasRole("ROLE_ADMIN") || membershipRepository.isManagerOf(viewer.getId(), condominiumId)) {
            return;
        }
        List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(viewer.getId());
        if (!myUnitIds.contains(unitId)) {
            throw new AccessDeniedException("Você não possui vínculo com esta unidade");
        }
    }
}
