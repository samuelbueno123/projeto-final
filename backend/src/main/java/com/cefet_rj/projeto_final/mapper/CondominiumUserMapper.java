package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.CondominiumUserDTO;
import com.cefet_rj.projeto_final.entities.CondominiumUser;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CondominiumUserMapper {

    void copyDtoToEntity(CondominiumUserDTO dto, @MappingTarget CondominiumUser entity);

}