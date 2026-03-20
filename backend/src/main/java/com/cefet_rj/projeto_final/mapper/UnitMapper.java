package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.UnitDTO;
import com.cefet_rj.projeto_final.entities.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UnitMapper {

    void copyDtoToEntity(UnitDTO dto, @MappingTarget Unit entity);

}