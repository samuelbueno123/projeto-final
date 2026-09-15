package com.cefet_rj.projeto_final.announcements.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AnnouncementUpdateDTO(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        String content,

        LocalDateTime expiresAt

) {
}