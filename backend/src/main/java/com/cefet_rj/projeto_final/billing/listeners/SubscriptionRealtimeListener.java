package com.cefet_rj.projeto_final.billing.listeners;

import com.cefet_rj.projeto_final.billing.events.SubscriptionUpdatedEvent;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Broadcasts SUBSCRIPTION_UPDATED SSE events strictly after transaction commit
 * so active dashboards refresh plan entitlements immediately without manual reload.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionRealtimeListener {

    private final RealtimeEmitterService realtimeEmitterService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(SubscriptionUpdatedEvent event) {
        if (event == null || event.condominiumId() == null) {
            return;
        }
        realtimeEmitterService.sendToCondominium(
                event.condominiumId(),
                RealtimeEventDTO.of(
                        "SUBSCRIPTION_UPDATED",
                        event.condominiumId(),
                        Map.of(
                                "planName", event.planName() != null ? event.planName() : "",
                                "status", event.status() != null ? event.status() : "",
                                "eventType", event.eventType() != null ? event.eventType() : "UPDATED"
                        )
                )
        );
    }
}
