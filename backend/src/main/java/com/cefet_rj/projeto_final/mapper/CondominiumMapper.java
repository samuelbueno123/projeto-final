package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.dtos.insert.CondominiumInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.CondominiumUpdateDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CondominiumMapper {

    void copyDtoToEntity(CondominiumDTO dto, @MappingTarget Condominium entity);

    void copyInsertDtoToEntity(CondominiumInsertDTO dto, @MappingTarget Condominium entity);

    void copyUpdateDtoToEntity(CondominiumUpdateDTO dto, @MappingTarget Condominium entity);

}