package com.cefet_rj.projeto_final.packages.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.packages.dtos.PackageDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageInsertDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageUpdateDTO;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.projections.PackageProjection;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.InvalidPackageStateTransitionException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.shared.util.Utils;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PackageService {

	private final PackageRepository packageRepository;

	private final UserRepository userRepository;

	private final UnitRepository unitRepository;

    private final AuthenticatedUserService authenticatedUserService;

    private final CondominiumAccessService condominiumAccessService;

    @Transactional(readOnly = true)
    public PackageDTO findById(Long id) {
        Optional<Package> obj = packageRepository.findById(id);
        Package entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return new PackageDTO(entity);
    }

    private void changeStatus(Package entity, PackageStatus nextStatus) {
        PackageStatus currentStatus = entity.getStatus();

        if (currentStatus == null) {
            throw new InvalidPackageStateTransitionException("Package has no current status");
        }

        if (!currentStatus.canChangeTo(nextStatus)) {
            throw new InvalidPackageStateTransitionException(
                    "Invalid package status transition from " + currentStatus + " to " + nextStatus
            );
        }

        entity.setStatus(nextStatus);
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

        changeStatus(entity, PackageStatus.RECEIVED);
        entity.setReceivedAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO notify(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        changeStatus(entity, PackageStatus.NOTIFIED);
        entity.setNotifiedAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO pickUp(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        changeStatus(entity, PackageStatus.PICKED_UP);
        entity.setPickedUpAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO cancel(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        changeStatus(entity, PackageStatus.CANCELLED);
        entity.setCancelledAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

    @Transactional
    public PackageDTO returnPkg(Long id) {
        Package entity = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        changeStatus(entity, PackageStatus.RETURNED);
        entity.setReturnedAt(LocalDateTime.now());

        return new PackageDTO(entity);
    }

	@Transactional
	public PackageDTO update(Long id, PackageUpdateDTO dto){
		try {
            Package entity = packageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

			linkAssociations(dto.description(), dto.recipientId(), dto.unitId(), entity);
			entity = packageRepository.save(entity);
			return new PackageDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!packageRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			packageRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	@SuppressWarnings(value = "unchecked")
	@Transactional(readOnly = true)
	public Page<PackageDTO> findAllPaged(String search, String status, Pageable pageable) {

		PackageStatus packageStatus = null;

		if (!status.isBlank()) {
			packageStatus = PackageStatus.fromString(status);
		}

		Page<PackageProjection> page = packageRepository.searchPackages(
				search,
				packageStatus == null ? null : packageStatus.name(),
				pageable
		);

        List<Long> packageIds = page.map(PackageProjection::getId).toList();

        if (packageIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Package> entities = packageRepository.searchPackagesWithRelations(packageIds);

		entities = (List<Package>) Utils.replace(page.getContent(), entities);

		List<PackageDTO> dtos = entities.stream()
				.map(PackageDTO::new)
				.toList();

		return new PageImpl<>(dtos, page.getPageable(), page.getTotalElements());
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

	private void linkAssociations(
            String description,
			Long recipientId,
            Long unitId,
            Package entity
    ) {
		entity.setDescription(description);

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipientId));
        entity.setRecipient(recipient);

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + unitId));
        entity.setUnit(unit);
	}
}
