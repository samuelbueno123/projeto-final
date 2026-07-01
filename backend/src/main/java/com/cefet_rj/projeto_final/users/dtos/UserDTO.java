package com.cefet_rj.projeto_final.users.dtos;

import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

public record UserDTO(

        Long id,

        @NotBlank(message = "Campo obrigatório")
        String firstName,

        String lastName,

        @NotBlank(message = "Favor entrar com um email válido")
        String email,

        Set<RoleDTO> roles

) {

    public UserDTO(User entity) {
        this(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getRoles()
                        .stream()
                        .map(RoleDTO::new)
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

    public UserDTO {
        if (roles == null) {
            roles = new HashSet<>();
        }
    }
}
