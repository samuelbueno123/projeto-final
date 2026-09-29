package com.cefet_rj.projeto_final.reservations.dtos;

import com.cefet_rj.projeto_final.reservations.entities.CommonArea;

public record CommonAreaDTO(

        Long id,
        String name,
        String description,
        Integer capacity,
        Boolean active,
        Long condominiumId

) {

    public CommonAreaDTO(CommonArea entity) {
        this(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCapacity(),
                entity.getActive(),
                entity.getCondominium().getId()
        );
    }
}