package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public record UserUpdateDTO(

        Long id,

        @NotBlank(message = "Campo obrigatório")
        String firstName,

        String lastName,

        @NotBlank(message = "Favor entrar com um email válido")
        String email,

        @NotBlank(message = "Campo obrigatório")
        @Size(min = 8, message = "Deve ter no mínimo 8 caracteres")
        String password,

        Set<RoleDTO> roles

) {

    public UserUpdateDTO(User entity) {
        this(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRoles()
                        .stream()
                        .map(RoleDTO::new)
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

    public UserUpdateDTO {
        if (roles == null) {
            roles = new HashSet<>();
        }
    }
}
