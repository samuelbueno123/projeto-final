package com.cefet_rj.projeto_final.dtos.insert;

import com.cefet_rj.projeto_final.dtos.RoleDTO;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.services.validation.UserInsertValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@UserInsertValid
public record UserInsertDTO(

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

    public UserInsertDTO(User entity) {
        this(
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

    public UserInsertDTO {
        if (roles == null) {
            roles = new HashSet<>();
        }
    }
}
