package com.cefet_rj.projeto_final.manager.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public ManagerDashboardDTO dashboard(@RequestParam Long condominiumId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId).orElseThrow();
        var units = unitRepository.findAllForCondominium(condominiumId).stream().map(unit -> {
            var payment = paymentRepository.findByUnitId(unit.getId()).orElse(null);
            return new ManagerDashboardDTO.UnitInfo(
                    unit.getId(),
                    unit.getIdentifier(),
                    unit.getSector().getName(),
                    payment == null ? "NOT_RECORDED" : payment.getStatus().name(),
                    payment == null ? null : payment.getDueDate(),
                    payment == null ? null : payment.getAmount(),
                    payment == null ? null : payment.getPaymentNote(),
                    payment == null ? null : payment.getRequestedAt()
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
                        r.getRejectionReason()
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
        if (dto.paymentNote() != null) {
            payment.setPaymentNote(dto.paymentNote().isBlank() ? null : dto.paymentNote().trim());
        }
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setUpdatedBy(viewer);
        paymentRepository.save(payment);

        // Notify residents of the unit when status changes
        List<Resident> unitResidents = residentRepository.findAllByUnitIdWithUser(unitId);
        String statusLabel = switch (dto.status()) {
            case PAID -> "PAGO (Confirmado)";
            case PENDING -> "PENDENTE";
            case OVERDUE -> "EM ATRASO";
            case CONFIRMATION_REQUESTED -> "EM ANÁLISE";
        };
        for (Resident res : unitResidents) {
            notificationService.createDirectNotification(
                    res.getUser(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Situação financeira da unidade " + unit.getIdentifier(),
                    "O status financeiro da sua unidade (" + unit.getSector().getName() + " • " + unit.getIdentifier()
                            + ") foi atualizado para: " + statusLabel + ".",
                    condominiumId,
                    unit.getSector().getCondominium().getName()
            );
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/condominiums/{condominiumId}/units/my-payments")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ManagerDashboardDTO.UnitInfo>> getMyUnitPayments(
            @PathVariable Long condominiumId
    ) {
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!user.hasRole("ROLE_ADMIN")) {
            condominiumAccessService.validateUserBelongsToCondominium(user.getId(), condominiumId);
        }
        List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());
        var allUnits = unitRepository.findAllForCondominium(condominiumId);
        var filtered = allUnits.stream()
                .filter(u -> myUnitIds.contains(u.getId()))
                .toList();
        var targetUnits = filtered.isEmpty() ? allUnits.stream().limit(1).toList() : filtered;

        var infos = targetUnits.stream().map(unit -> {
            var payment = paymentRepository.findByUnitId(unit.getId()).orElse(null);
            return new ManagerDashboardDTO.UnitInfo(
                    unit.getId(),
                    unit.getIdentifier(),
                    unit.getSector().getName(),
                    payment == null ? "NOT_RECORDED" : payment.getStatus().name(),
                    payment == null ? null : payment.getDueDate(),
                    payment == null ? null : payment.getAmount(),
                    payment == null ? null : payment.getPaymentNote(),
                    payment == null ? null : payment.getRequestedAt()
            );
        }).toList();

        return ResponseEntity.ok(infos);
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

        return ResponseEntity.ok(new ManagerDashboardDTO.UnitInfo(
                unit.getId(),
                unit.getIdentifier(),
                unit.getSector().getName(),
                payment.getStatus().name(),
                payment.getDueDate(),
                payment.getAmount(),
                payment.getPaymentNote(),
                payment.getRequestedAt()
        ));
    }

    @GetMapping("/manager/condominiums/{condominiumId}/sectors")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<SectorDTO>> listSectors(@PathVariable Long condominiumId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        var list = sectorRepository.findAll().stream()
                .filter(s -> s.getCondominium().getId().equals(condominiumId))
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
        Unit unit = new Unit();
        unit.setIdentifier(identifier);
        unit.setSector(sector);
        unit = unitRepository.save(unit);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UnitDTO(unit));
    }

    private void ensureAccess(User viewer, Long condominiumId) {
        if (!viewer.hasRole("ROLE_ADMIN") && !membershipRepository.isManagerOf(viewer.getId(), condominiumId))
            throw new AccessDeniedException("Você não administra este condomínio");
    }
}
