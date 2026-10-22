package com.cefet_rj.projeto_final.manager.controllers;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.manager.dtos.ManagerDashboardDTO;
import com.cefet_rj.projeto_final.manager.dtos.UnitPaymentUpdateDTO;
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController @RequestMapping("/manager") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ManagerDashboardController {
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumRepository condominiumRepository;
    private final MembershipRepository membershipRepository;
    private final UnitRepository unitRepository;
    private final UnitPaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping("/dashboard") @Transactional(readOnly = true)
    public ManagerDashboardDTO dashboard(@RequestParam Long condominiumId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Condominium condo = condominiumRepository.findById(condominiumId).orElseThrow();
        var units = unitRepository.findAllForCondominium(condominiumId).stream().map(unit -> {
            var payment = paymentRepository.findByUnitId(unit.getId()).orElse(null);
            return new ManagerDashboardDTO.UnitInfo(unit.getId(), unit.getIdentifier(), unit.getSector().getName(),
                    payment == null ? "NOT_RECORDED" : payment.getStatus().name(),
                    payment == null ? null : payment.getDueDate(), payment == null ? null : payment.getAmount());
        }).toList();
        var reservations = reservationRepository.findByCommonAreaCondominiumIdOrderByStartAtDesc(condominiumId)
                .stream().map(r -> new ManagerDashboardDTO.ReservationInfo(r.getId(), r.getStartAt(), r.getEndAt(),
                        r.getStatus().name(), r.getCommonArea().getName(),
                        r.getReservedBy().getFirstName() + " " + r.getReservedBy().getLastName(), r.getReservedBy().getEmail()))
                .toList();
        return new ManagerDashboardDTO(condominiumId, condo.getName(), units.size(),
                membershipRepository.countDistinctUsers(condominiumId), reservations.size(), units, reservations);
    }

    @PutMapping("/condominiums/{condominiumId}/units/{unitId}/payment-status") @Transactional
    public ResponseEntity<Void> updatePayment(@PathVariable Long condominiumId, @PathVariable Long unitId,
            @Valid @RequestBody UnitPaymentUpdateDTO dto) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        ensureAccess(viewer, condominiumId);
        Unit unit = unitRepository.findById(unitId).orElseThrow();
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) throw new AccessDeniedException("Unidade fora deste condomínio");
        UnitPayment payment = paymentRepository.findByUnitId(unitId).orElseGet(UnitPayment::new);
        payment.setUnit(unit); payment.setStatus(dto.status()); payment.setDueDate(dto.dueDate());
        payment.setAmount(dto.amount()); payment.setUpdatedAt(LocalDateTime.now()); payment.setUpdatedBy(viewer);
        paymentRepository.save(payment);
        return ResponseEntity.noContent().build();
    }

    private void ensureAccess(User viewer, Long condominiumId) {
        if (!viewer.hasRole("ROLE_ADMIN") && !membershipRepository.isManagerOf(viewer.getId(), condominiumId))
            throw new AccessDeniedException("Você não administra este condomínio");
    }
}
