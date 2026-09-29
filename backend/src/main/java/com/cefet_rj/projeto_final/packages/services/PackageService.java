package com.cefet_rj.projeto_final.packages.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.packages.dtos.PackageDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageInsertDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageUpdateDTO;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.events.PackageStatusChangedEvent;
import com.cefet_rj.projeto_final.packages.history.services.PackageHistoryService;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.InvalidPackageStateTransitionException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PackageService {

	private final PackageRepository packageRepository;
	private final UserRepository userRepository;
	private final UnitRepository unitRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final PackageHistoryService packageHistoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final CondominiumFeatureAccessService featureAccessService;

    @Transactional(readOnly = true)
    public PackageDTO findById(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validatePackageAccess(entity);

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

        linkAssociations(dto.description(), dto.recipientId(), dto.unitId(), entity);

        entity.setStatus(PackageStatus.RECEIVED);
        entity.setReceivedAt(LocalDateTime.now());

        entity = packageRepository.save(entity);
        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO registerExpected(PackageInsertDTO dto) {
        Package entity = new Package();

        linkAssociations(dto.description(), dto.recipientId(), dto.unitId(), entity);

        entity.setStatus(PackageStatus.EXPECTED);

        entity = packageRepository.save(entity);
        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO receive(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validatePackageAccess(entity);

        changeStatus(
                entity,
                PackageStatus.RECEIVED,
                "Package received at condominium"
        );
        entity.setReceivedAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO notify(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validatePackageAccess(entity);

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

        validatePackageAccess(entity);

        changeStatus(
                entity,
                PackageStatus.PICKED_UP,
                "Package picked up"
        );
        entity.setPickedUpAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO cancel(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validatePackageAccess(entity);

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

        validatePackageAccess(entity);

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

        validatePackageAccess(entity);

        linkAssociations(
                dto.description(),
                dto.recipientId(),
                dto.unitId(),
                entity
        );

        return new PackageDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        try {
            Package entity = packageRepository.findById(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Package not found: " + id));

            validatePackageAccess(entity);

            packageRepository.delete(entity);
        }
        catch (DataIntegrityViolationException e) {
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
        User user = authenticatedUserService.getAuthenticatedUser();

        if (!user.hasRole("ROLE_ADMIN")) {
            condominiumAccessService.validateUserBelongsToCondominium(
                    user.getId(),
                    condominiumId
            );
        }

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

    private void validatePackageAccess(Package packageEntity) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        if (authenticatedUser.hasRole("ROLE_ADMIN")) {
            return;
        }

        Long condominiumId = packageEntity
                .getUnit()
                .getSector()
                .getCondominium()
                .getId();

        condominiumAccessService.validateUserBelongsToCondominium(
                authenticatedUser.getId(),
                condominiumId
        );
    }

    private void validateUnitAccess(Unit unit) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        if (authenticatedUser.hasRole("ROLE_ADMIN")) {
            return;
        }

        Long condominiumId = unit
                .getSector()
                .getCondominium()
                .getId();

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

        Long condominiumId = entity
                .getUnit()
                .getSector()
                .getCondominium()
                .getId();

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
            String description,
            Long recipientId,
            Long unitId,
            Package entity
    ) {
        entity.setDescription(description);

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + recipientId));

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Unit not found: " + unitId
                        )
                );

        validateUnitAccess(unit);

        featureAccessService.validateFeatureEnabled(
                getCondominiumId(unit),
                FeatureCode.PACKAGES
        );

        entity.setRecipient(recipient);
        entity.setUnit(unit);
    }
}
