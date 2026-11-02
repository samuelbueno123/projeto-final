package com.cefet_rj.projeto_final.community.dtos;

import com.cefet_rj.projeto_final.community.entities.DoormanChatMessage;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record ChatMessageDTO(
        Long id,
        Long condominiumId,
        Long senderId,
        String senderName,
        String senderRole,
        Long recipientId,
        String recipientName,
        Long unitId,
        String unitLabel,
        String message,
        LocalDateTime sentAt,
        Boolean isRead
) {
    public ChatMessageDTO(DoormanChatMessage entity) {
        this(
                entity.getId(),
                entity.getCondominium().getId(),
                entity.getSender().getId(),
                (entity.getSender().getFirstName() + " " + entity.getSender().getLastName()).trim(),
                entity.getSenderRole(),
                entity.getRecipient() != null ? entity.getRecipient().getId() : null,
                entity.getRecipient() != null
                        ? (entity.getRecipient().getFirstName() + " " + entity.getRecipient().getLastName()).trim()
                        : null,
                entity.getUnit() != null ? entity.getUnit().getId() : null,
                entity.getUnit() != null
                        ? (entity.getUnit().getSector().getName() + " • Unid. " + entity.getUnit().getIdentifier())
                        : null,
                entity.getMessage(),
                entity.getSentAt(),
                entity.getIsRead()
        );
    }

    public record SendMessageDTO(
            Long recipientId,
            Long unitId,
            @NotBlank String message
    ) {}
}
