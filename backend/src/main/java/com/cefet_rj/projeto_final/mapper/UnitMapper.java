package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.UnitDTO;
import com.cefet_rj.projeto_final.dtos.insert.UnitInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.UnitUpdateDTO;
import com.cefet_rj.projeto_final.entities.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UnitMapper {

    void copyDtoToEntity(UnitDTO dto, @MappingTarget Unit entity);

    void copyInsertDtoToEntity(UnitInsertDTO dto, @MappingTarget Unit entity);

    void copyUpdateDtoToEntity(UnitUpdateDTO dto, @MappingTarget Unit entity);

}