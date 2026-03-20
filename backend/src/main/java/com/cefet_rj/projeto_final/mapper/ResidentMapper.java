package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.entities.Resident;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ResidentMapper {

    void copyDtoToEntity(ResidentDTO dto, @MappingTarget Resident entity);

}