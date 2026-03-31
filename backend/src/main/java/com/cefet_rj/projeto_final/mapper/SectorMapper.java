package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.SectorUpdateDTO;
import com.cefet_rj.projeto_final.entities.Sector;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SectorMapper {

    void copyDtoToEntity(SectorDTO dto, @MappingTarget Sector entity);

    void copyInsertDtoToEntity(SectorInsertDTO dto, @MappingTarget Sector entity);

    void copyUpdateDtoToEntity(SectorUpdateDTO dto, @MappingTarget Sector entity);

}