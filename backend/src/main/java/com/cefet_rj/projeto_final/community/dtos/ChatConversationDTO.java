package com.cefet_rj.projeto_final.community.dtos;

import java.time.LocalDateTime;

public record ChatConversationDTO(
        Long userId,
        String userName,
        Long unitId,
        String unitLabel,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
