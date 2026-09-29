package com.cefet_rj.projeto_final.billing.dtos;

import com.cefet_rj.projeto_final.billing.entities.Feature;

public record FeatureDTO(

        Long id,
        String code,
        String name,
        String description

) {

    public FeatureDTO(Feature entity) {
        this(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription()
        );
    }
}