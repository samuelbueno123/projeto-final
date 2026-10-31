package com.cefet_rj.projeto_final.announcements.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AnnouncementRequestDTO(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        String content,

        LocalDateTime expiresAt,

        @jakarta.validation.constraints.Size(max = 1500000)
        String imageUrl,

        /** Unidade destinatária. Nulo = comunicado para todo o condomínio. */
        Long unitId

) {
    public AnnouncementRequestDTO(
            String title,
            String content,
            LocalDateTime expiresAt
    ) {
        this(title, content, expiresAt, null, null);
    }

    public AnnouncementRequestDTO(
            String title,
            String content,
            LocalDateTime expiresAt,
            String imageUrl
    ) {
        this(title, content, expiresAt, imageUrl, null);
    }
}
