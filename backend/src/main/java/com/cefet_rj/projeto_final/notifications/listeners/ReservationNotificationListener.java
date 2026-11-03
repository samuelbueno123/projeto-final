package com.cefet_rj.projeto_final.notifications.listeners;

import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.reservations.events.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReservationNotificationListener {

    private final NotificationService notificationService;
    private final CondominiumFeatureAccessService featureAccessService;
    private final RealtimeEmitterService realtimeEmitterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ReservationCreatedEvent event) {

        // Transmissão SSE em tempo real para o condomínio e o solicitante
        realtimeEmitterService.sendToCondominium(event.condominiumId(),
                RealtimeEventDTO.of("RESERVATION_UPDATED", event.condominiumId(), Map.of(
                        "reservationId", event.reservationId(),
                        "commonAreaName", event.commonAreaName(),
                        "startAt", event.startAt().toString()
                )));

        if (!featureAccessService.hasFeature(
                event.condominiumId(),
                FeatureCode.NOTIFICATIONS
        )) {
            return;
        }

        notificationService.createReservationNotification(
                event.recipientId(),
                event.commonAreaName(),
                event.startAt()
        );
    }
}
