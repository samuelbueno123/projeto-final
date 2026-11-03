package com.cefet_rj.projeto_final.realtime.dtos;

import java.time.LocalDateTime;

public record RealtimeEventDTO(
        String type,
        Long condominiumId,
        Object data,
        LocalDateTime timestamp
) {
    public static RealtimeEventDTO of(String type, Long condominiumId, Object data) {
        return new RealtimeEventDTO(type, condominiumId, data, LocalDateTime.now());
    }

    public static RealtimeEventDTO of(String type, Object data) {
        return new RealtimeEventDTO(type, null, data, LocalDateTime.now());
    }
}
