package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.entities.Membership;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MembershipMapper {

    void copyDtoToEntity(MembershipDTO dto, @MappingTarget Membership entity);

}