package com.cefet_rj.projeto_final.condominiums.security;

import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CondominiumFeatureAccessService {

    private final CondominiumRepository condominiumRepository;

    public void validateFeatureEnabled(
            Long condominiumId,
            FeatureCode featureCode
    ) {
        if (!hasFeature(condominiumId, featureCode)) {
            throw new ForbiddenException(
                    "Feature " + featureCode
                            + " is not enabled for condominium "
                            + condominiumId
            );
        }
    }

    public boolean hasFeature(
            Long condominiumId,
            FeatureCode featureCode
    ) {
        Condominium condominium = condominiumRepository
                .findById(condominiumId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Condominium not found: " + condominiumId
                        )
                );

        return condominium
                .getBillingPlan()
                .getFeatures()
                .stream()
                .anyMatch(feature ->
                        feature.getCode()
                                .equalsIgnoreCase(featureCode.name())
                );
    }
}