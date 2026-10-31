package com.cefet_rj.projeto_final.notifications.dtos;

import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        NotificationType type,
        /**
         * Origem da notificação: SYSTEM (automática do sistema), ADMIN
         * (comunicado do administrador) ou CONDOMINIUM (comunicado do condomínio).
         */
        String origin,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt,
        String imageUrl,
        Long invitationId,
        Long condominiumId,
        String condominiumName,
        String authority,
        String invitationStatus
) {
    public NotificationDTO(Notification entity) {
        this(
                entity.getId(),
                entity.getType(),
                originOf(entity.getType()),
                entity.getTitle(),
                entity.getMessage(),
                entity.getRead(),
                entity.getCreatedAt(),
                entity.getImageUrl(),
                entity.getInvitationId(),
                entity.getCondominiumId(),
                entity.getCondominiumName(),
                entity.getAuthority(),
                entity.getInvitationStatus()
        );
    }

    private static String originOf(NotificationType type) {
        if (type == null) {
            return "SYSTEM";
        }
        return switch (type) {
            case SYSTEM_ANNOUNCEMENT -> "ADMIN";
            case ANNOUNCEMENT -> "CONDOMINIUM";
            default -> "SYSTEM";
        };
    }
}