package com.cefet_rj.projeto_final.condominiums.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MembershipInvitationRequestDTO(
        @NotBlank(message = "Campo obrigatório")
        @Email(message = "Informe um email válido")
        String email,

        @NotBlank(message = "Campo obrigatório")
        String authority,

        Long unitId,

        String residentType
) {
    public MembershipInvitationRequestDTO(String email, String authority) {
        this(email, authority, null, null);
    }
}
