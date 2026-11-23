package com.cefet_rj.projeto_final.billing.events;

public record SubscriptionUpdatedEvent(
        Long condominiumId,
        String planName,
        String status,
        String eventType
) {
}
