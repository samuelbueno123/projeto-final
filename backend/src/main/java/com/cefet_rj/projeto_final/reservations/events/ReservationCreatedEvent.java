package com.cefet_rj.projeto_final.reservations.events;

import java.time.LocalDateTime;

public record ReservationCreatedEvent(

        Long reservationId,
        Long condominiumId,
        Long recipientId,
        String commonAreaName,
        LocalDateTime startAt,
        LocalDateTime occurredAt

) {
}