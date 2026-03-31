package com.cefet_rj.projeto_final.mapper;

import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.dtos.insert.UserInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.UserUpdateDTO;
import com.cefet_rj.projeto_final.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    void copyInsertDtoToEntity(UserInsertDTO dto, @MappingTarget User entity);

    void copyUpdateDtoToEntity(UserUpdateDTO dto, @MappingTarget User entity);

    void copyDtoToEntity(UserDTO dto, @MappingTarget User entity);

}