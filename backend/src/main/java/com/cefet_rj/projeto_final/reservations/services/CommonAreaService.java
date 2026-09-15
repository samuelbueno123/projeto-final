package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaDTO;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaRequestDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaUpdateDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonAreaService {

    private final CommonAreaRepository commonAreaRepository;
    private final CondominiumRepository condominiumRepository;

    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;

    @Transactional
    public CommonAreaDTO create(
            Long condominiumId,
            CommonAreaRequestDTO dto
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        Condominium condominium =
                condominiumRepository.findById(condominiumId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Condominium not found: "
                                                + condominiumId
                                )
                        );

        CommonArea commonArea = new CommonArea();

        commonArea.setName(dto.name().trim());
        commonArea.setDescription(
                dto.description() == null
                        ? null
                        : dto.description().trim()
        );
        commonArea.setCapacity(dto.capacity());
        commonArea.setActive(true);
        commonArea.setCondominium(condominium);

        commonArea =
                commonAreaRepository.save(commonArea);

        return new CommonAreaDTO(commonArea);
    }

    @Transactional(readOnly = true)
    public List<CommonAreaDTO> findActiveByCondominium(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        return commonAreaRepository
                .findByCondominiumIdAndActiveTrueOrderByNameAsc(
                        condominiumId
                )
                .stream()
                .map(CommonAreaDTO::new)
                .toList();
    }

    private void validateCondominiumAccess(
            Long condominiumId
    ) {

        User user =
                authenticatedUserService.getAuthenticatedUser();

        if (user.hasRole("ROLE_ADMIN")) {
            return;
        }

        condominiumAccessService
                .validateUserBelongsToCondominium(
                        user.getId(),
                        condominiumId
                );
    }

    @Transactional
    public CommonAreaDTO update(
            Long id,
            CommonAreaUpdateDTO dto
    ) {

        CommonArea commonArea =
                findCommonAreaById(id);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        commonArea.setName(
                dto.name().trim()
        );

        commonArea.setDescription(
                dto.description() == null
                        ? null
                        : dto.description().trim()
        );

        commonArea.setCapacity(
                dto.capacity()
        );

        commonArea =
                commonAreaRepository.save(commonArea);

        return new CommonAreaDTO(commonArea);
    }

    @Transactional
    public void activate(Long id) {

        CommonArea commonArea =
                findCommonAreaById(id);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        commonArea.setActive(true);

        commonAreaRepository.save(commonArea);
    }

    @Transactional
    public void deactivate(Long id) {

        CommonArea commonArea =
                findCommonAreaById(id);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        commonArea.setActive(false);

        commonAreaRepository.save(commonArea);
    }

    private CommonArea findCommonAreaById(
            Long id
    ) {

        return commonAreaRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Common area not found: " + id
                        )
                );
    }
}