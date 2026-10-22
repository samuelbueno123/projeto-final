package com.cefet_rj.projeto_final.users.dtos.update;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateDTO(
        String currentPassword,
        String googleIdToken,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {}
