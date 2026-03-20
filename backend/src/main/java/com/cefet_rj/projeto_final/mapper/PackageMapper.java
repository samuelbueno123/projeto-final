package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.entities.Package;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PackageMapper {

    void copyDtoToEntity(PackageDTO dto, @MappingTarget Package entity);

}