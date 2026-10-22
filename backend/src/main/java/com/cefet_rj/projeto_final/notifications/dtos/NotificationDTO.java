package com.cefet_rj.projeto_final.notifications.dtos;

import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        NotificationType type,
        String title,
        String message,
        Boolean read,
        LocalDateTime createdAt,
        String imageUrl
) {
    public NotificationDTO(Notification entity) {
        this(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getRead(),
                entity.getCreatedAt(),
                entity.getImageUrl()
        );
    }
}
