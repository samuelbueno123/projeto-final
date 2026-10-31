package com.cefet_rj.projeto_final.condominiums.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddManagerRequestDTO(
        @NotBlank(message = "Campo obrigatório")
        @Email(message = "Informe um email válido")
        String email
) {
}
