package com.cefet_rj.projeto_final.dtos;

import com.cefet_rj.projeto_final.entities.Role;

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
