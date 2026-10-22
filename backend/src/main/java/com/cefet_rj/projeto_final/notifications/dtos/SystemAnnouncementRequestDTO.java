package com.cefet_rj.projeto_final.notifications.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemAnnouncementRequestDTO(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 5000) String message,
        @Size(max = 1500000) String imageUrl
) {}
