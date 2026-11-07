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

        /** Unidade destinatária. Nulo = comunicado para todo o condomínio/bloco. */
        Long unitId,

        /** Setor/Bloco destinatário. Nulo = todos os blocos. */
        Long sectorId,

        /** Perfil destinatário (ex: ROLE_RESIDENT, ROLE_DOORMAN). Nulo = todos. */
        String targetRole

) {
    public AnnouncementRequestDTO(
            String title,
            String content,
            LocalDateTime expiresAt
    ) {
        this(title, content, expiresAt, null, null, null, null);
    }

    public AnnouncementRequestDTO(
            String title,
            String content,
            LocalDateTime expiresAt,
            String imageUrl
    ) {
        this(title, content, expiresAt, imageUrl, null, null, null);
    }

    public AnnouncementRequestDTO(
            String title,
            String content,
            LocalDateTime expiresAt,
            String imageUrl,
            Long unitId
    ) {
        this(title, content, expiresAt, imageUrl, unitId, null, null);
    }
}
