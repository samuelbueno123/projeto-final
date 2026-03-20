package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.services.validation.UserUpdateValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@UserUpdateValid
public record UserUpdateDTO(

        @NotBlank(message = "Campo obrigatório")
        String firstName,

        String lastName,

        @NotBlank(message = "Favor entrar com um email válido")
        String email,


        Set<RoleDTO> roles

) {

    public UserUpdateDTO(User entity) {
        this(
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
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
