package com.cefet_rj.projeto_final.users.dtos;

import com.cefet_rj.projeto_final.users.entities.Role;

public record RoleDTO(
        Long id,
        String authority
) {

    public RoleDTO(Role entity) {
        this(
                entity.getId(),
                entity.getAuthority()
        );
    }
}
