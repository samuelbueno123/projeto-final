package com.cefet_rj.projeto_final.packages.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.packages.dtos.PackageDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageInsertDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageUpdateDTO;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.events.PackageStatusChangedEvent;
import com.cefet_rj.projeto_final.packages.history.repositories.PackageHistoryRepository;
import com.cefet_rj.projeto_final.packages.history.services.PackageHistoryService;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.InvalidPackageStateTransitionException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.shared.observability.OperationalMetricsService;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final MembershipRepository membershipRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final PackageHistoryService packageHistoryService;
    private final PackageHistoryRepository packageHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CondominiumFeatureAccessService featureAccessService;

    @Autowired(required = false)
    private OperationalMetricsService operationalMetricsService;

    @Transactional(readOnly = true)
    public PackageDTO findById(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validatePackageOwnerOrStaffAccess(entity);

        return new PackageDTO(entity);
    }

    private void changeStatus(
            Package entity,
            PackageStatus nextStatus,
            String notes
    ) {
        PackageStatus currentStatus = entity.getStatus();

        if (currentStatus == null) {
            throw new InvalidPackageStateTransitionException(
                    "Package has no current status"
            );
        }

        if (!currentStatus.canChangeTo(nextStatus)) {
            throw new InvalidPackageStateTransitionException(
                    "Invalid package status transition from "
                            + currentStatus
                            + " to "
                            + nextStatus
            );
        }

        entity.setStatus(nextStatus);

        packageHistoryService.registerUserAction(
                entity,
                currentStatus,
                nextStatus,
                notes
        );

        publishStatusChangedEvent(
                entity,
                currentStatus,
                nextStatus
        );
    }

    @Transactional
    public PackageDTO registerReceived(PackageInsertDTO dto) {
        Package entity = new Package();

        linkAssociations(dto.condominiumId(), dto.description(), dto.recipientId(), dto.unitId(), entity);
        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        entity.setStatus(PackageStatus.RECEIVED);
        entity.setReceivedAt(LocalDateTime.now());

        entity = packageRepository.save(entity);
        packageHistoryService.registerUserAction(
                entity,
                null,
                PackageStatus.RECEIVED,
                "Encomenda recebida na portaria"
        );
        publishStatusChangedEvent(entity, null, PackageStatus.RECEIVED);
        if (operationalMetricsService != null) {
            operationalMetricsService.recordPackageReceived();
        }
        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO registerExpected(PackageInsertDTO dto) {
        Package entity = new Package();
        User caller = authenticatedUserService.getAuthenticatedUser();
        Long recipientId = dto.recipientId();

        Unit tempUnit = null;
        if (dto.unitId() != null) {
            tempUnit = unitRepository.findById(dto.unitId()).orElse(null);
        }
        Long targetCondoId = dto.condominiumId() != null
                ? dto.condominiumId()
                : (tempUnit != null ? getCondominiumId(tempUnit) : null);

        boolean isStaff = targetCondoId != null
                ? condominiumAccessService.isStaffOrAdmin(caller, targetCondoId)
                : caller.hasRole("ROLE_ADMIN");

        if (!isStaff) {
            recipientId = caller.getId();
            if (dto.unitId() != null) {
                List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(caller.getId());
                if (!myUnitIds.contains(dto.unitId())) {
                    throw new ForbiddenException("Você só pode cadastrar encomendas esperadas para sua própria unidade.");
                }
            }
        }

        linkAssociations(dto.condominiumId(), dto.description(), recipientId, dto.unitId(), entity);

        entity.setStatus(PackageStatus.EXPECTED);

        entity = packageRepository.save(entity);
        packageHistoryService.registerUserAction(
                entity,
                null,
                PackageStatus.EXPECTED,
                "Encomenda esperada cadastrada"
        );
        publishStatusChangedEvent(entity, null, PackageStatus.EXPECTED);
        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO requestPickup(Long id, String note) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
        validatePackageOwnerOrStaffAccess(entity);
        String cleanNote = note == null || note.isBlank()
                ? "Morador solicitou separação da encomenda para retirada na portaria."
                : "Solicitação de retirada: " + note.trim();
        packageHistoryService.registerUserAction(
                entity,
                entity.getStatus(),
                entity.getStatus(),
                cleanNote
        );
        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO receive(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        changeStatus(
                entity,
                PackageStatus.RECEIVED,
                "Package received at condominium"
        );
        entity.setReceivedAt(LocalDateTime.now());
        if (operationalMetricsService != null) {
            operationalMetricsService.recordPackageReceived();
        }

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO notify(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        changeStatus(
                entity,
                PackageStatus.NOTIFIED,
                "Resident notified about package"
        );
        entity.setNotifiedAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO pickUp(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        changeStatus(
                entity,
                PackageStatus.PICKED_UP,
                "Package picked up"
        );
        entity.setPickedUpAt(LocalDateTime.now());
        if (operationalMetricsService != null) {
            operationalMetricsService.recordPackageDelivered();
        }

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO cancel(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validatePackageOwnerOrStaffAccess(entity);

        User caller = authenticatedUserService.getAuthenticatedUser();
        Long condoId = getCondominiumId(entity.getUnit());
        if (!condominiumAccessService.isStaffOrAdmin(caller, condoId)
                && entity.getStatus() != PackageStatus.EXPECTED) {
            throw new ForbiddenException("Apenas a portaria ou administração pode cancelar encomendas já recebidas.");
        }

        changeStatus(
                entity,
                PackageStatus.CANCELLED,
                "Expected package cancelled"
        );
        entity.setCancelledAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO returnPkg(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        changeStatus(
                entity,
                PackageStatus.RETURNED,
                "Package returned"
        );
        entity.setReturnedAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO update(Long id, PackageUpdateDTO dto) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Package not found: " + id));

        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        Long effectiveRecipientId = dto.recipientId() != null
                ? dto.recipientId()
                : (entity.getRecipient() != null ? entity.getRecipient().getId() : null);
        Long effectiveUnitId = dto.unitId() != null
                ? dto.unitId()
                : entity.getUnit().getId();

        linkAssociations(
                null,
                dto.description(),
                effectiveRecipientId,
                effectiveUnitId,
                entity
        );
        condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

        return new PackageDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        try {
            Package entity = packageRepository.findById(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Package not found: " + id));

            condominiumAccessService.validateStaffAccess(getCondominiumId(entity.getUnit()));

            packageHistoryRepository.deleteByPackageEntityId(id);
            packageRepository.delete(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }
    }

    @Transactional(readOnly = true)
    public Page<PackageDTO> findAllPagedByCondominium(
            Long condominiumId,
            String search,
            String status,
            Pageable pageable
    ) {
        condominiumAccessService.validateStaffAccess(condominiumId);

        PackageStatus packageStatus = null;

        if (!status.isBlank()) {
            packageStatus = PackageStatus.fromString(status);
        }

        Page<Package> page = packageRepository.searchPackagesByCondominium(
                condominiumId,
                search,
                packageStatus,
                pageable
        );

        return page.map(PackageDTO::new);
    }

    @Transactional(readOnly = true)
    public Page<PackageDTO> findMyPackages(Pageable pageable) {
        User user = authenticatedUserService.getAuthenticatedUser();

        Page<Package> page = packageRepository.findByRecipientId(
                user.getId(),
                pageable
        );

        return page.map(PackageDTO::new);
    }

    private void validatePackageOwnerOrStaffAccess(Package packageEntity) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        if (authenticatedUser.hasRole("ROLE_ADMIN")) {
            return;
        }

        Long condominiumId = getCondominiumId(packageEntity.getUnit());

        condominiumAccessService.validateUserBelongsToCondominium(
                authenticatedUser.getId(),
                condominiumId
        );

        if (condominiumAccessService.isStaffOrAdmin(authenticatedUser, condominiumId)) {
            return;
        }

        boolean isRecipient = packageEntity.getRecipient() != null
                && packageEntity.getRecipient().getId().equals(authenticatedUser.getId());
        boolean isUnitResident = packageEntity.getUnit() != null
                && residentRepository.findUnitIdsByUserId(authenticatedUser.getId()).contains(packageEntity.getUnit().getId());

        if (!isRecipient && !isUnitResident) {
            throw new ForbiddenException("Você não possui permissão para acessar esta encomenda.");
        }
    }

    private void validateUnitAccess(Unit unit) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        if (authenticatedUser.hasRole("ROLE_ADMIN")) {
            return;
        }

        Long condominiumId = getCondominiumId(unit);

        condominiumAccessService.validateUserBelongsToCondominium(
                authenticatedUser.getId(),
                condominiumId
        );
    }

    private void publishStatusChangedEvent(
            Package entity,
            PackageStatus previousStatus,
            PackageStatus newStatus
    ) {
        Long condominiumId = getCondominiumId(entity.getUnit());

        PackageStatusChangedEvent event =
                new PackageStatusChangedEvent(
                        entity.getId(),
                        entity.getRecipient().getId(),
                        condominiumId,
                        previousStatus,
                        newStatus,
                        LocalDateTime.now()
                );

        eventPublisher.publishEvent(event);
    }

    private Long getCondominiumId(Unit unit) {
        return unit
                .getSector()
                .getCondominium()
                .getId();
    }

    private void linkAssociations(
            Long condominiumId,
            String description,
            Long recipientId,
            Long unitId,
            Package entity
    ) {
        entity.setDescription(description);

        Unit unit;
        if (unitId != null) {
            unit = unitRepository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + unitId));
        } else if (recipientId != null && condominiumId != null) {
            unit = residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(recipientId, condominiumId)
                    .map(Resident::getUnit)
                    .orElseGet(() -> unitRepository.findAllForCondominium(condominiumId).stream()
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma unidade válida.")));
        } else if (condominiumId != null) {
            unit = unitRepository.findAllForCondominium(condominiumId).stream()
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma unidade válida."));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a unidade da encomenda.");
        }

        validateUnitAccess(unit);

        Long resolvedCondoId = getCondominiumId(unit);
        if (condominiumId != null && !condominiumId.equals(resolvedCondoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade informada não pertence ao condomínio selecionado.");
        }

        User recipient;
        if (recipientId != null) {
            recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipientId));
            if (!recipient.hasRole("ROLE_ADMIN")
                    && !condominiumAccessService.userBelongsToCondominium(recipient.getId(), resolvedCondoId)
                    && residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(recipient.getId(), resolvedCondoId).isEmpty()) {
                throw new ForbiddenException("O destinatário informado não pertence a este condomínio.");
            }
        } else {
            recipient = residentRepository.findFirstByUnitId(unit.getId())
                    .map(Resident::getUser)
                    .orElseGet(() -> {
                        var residentUserIds = membershipRepository.findResidentUserIdsByCondominiumId(resolvedCondoId);
                        if (!residentUserIds.isEmpty()) {
                            return userRepository.findById(residentUserIds.get(0)).orElseGet(authenticatedUserService::getAuthenticatedUser);
                        }
                        return authenticatedUserService.getAuthenticatedUser();
                    });
        }

        featureAccessService.validateFeatureEnabled(
                resolvedCondoId,
                FeatureCode.PACKAGES
        );

        entity.setRecipient(recipient);
        entity.setUnit(unit);
    }
}
