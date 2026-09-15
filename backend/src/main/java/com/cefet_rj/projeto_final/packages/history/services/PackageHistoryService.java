package com.cefet_rj.projeto_final.packages.history.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.history.dtos.PackageHistoryDTO;
import com.cefet_rj.projeto_final.packages.history.entities.PackageHistory;
import com.cefet_rj.projeto_final.packages.history.enums.PackageHistoryActorType;
import com.cefet_rj.projeto_final.packages.history.repositories.PackageHistoryRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageHistoryService {

    private final PackageHistoryRepository packageHistoryRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PackageRepository packageRepository;
    private final CondominiumAccessService condominiumAccessService;

    @Transactional
    public void registerUserAction(
            Package packageEntity,
            PackageStatus previousStatus,
            PackageStatus newStatus,
            String notes
    ) {
        User user = authenticatedUserService.getAuthenticatedUser();

        PackageHistory history = new PackageHistory();

        history.setPackageEntity(packageEntity);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setPerformedBy(user);
        history.setActorType(PackageHistoryActorType.USER);
        history.setPerformedAt(LocalDateTime.now());
        history.setNotes(notes);

        packageHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<PackageHistoryDTO> findByPackageId(Long packageId) {
        validatePackageAccess(packageId);

        return packageHistoryRepository
                .findByPackageEntityIdOrderByPerformedAtAsc(packageId)
                .stream()
                .map(PackageHistoryDTO::new)
                .toList();
    }

    private Package validatePackageAccess(Long packageId) {
        Package packageEntity = packageRepository.findById(packageId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Package not found: " + packageId));

        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        if (authenticatedUser.hasRole("ROLE_ADMIN")) {
            return packageEntity;
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

        return packageEntity;
    }
}