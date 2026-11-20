package com.cefet_rj.projeto_final.notifications.listeners;

import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.events.PackageStatusChangedEvent;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PackageNotificationListener {

    private final NotificationService notificationService;
    private final CondominiumFeatureAccessService featureAccessService;
    private final RealtimeEmitterService realtimeEmitterService;
    private final MembershipRepository membershipRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PackageStatusChangedEvent event) {

        // Transmissão SSE em tempo real apenas para o destinatário e equipe da portaria/administração
        RealtimeEventDTO updateEvent = RealtimeEventDTO.of("PACKAGE_UPDATED", event.condominiumId(), Map.of(
                "packageId", event.packageId(),
                "newStatus", event.newStatus().name(),
                "previousStatus", event.previousStatus() != null ? event.previousStatus().name() : ""
        ));
        Set<Long> recipients = new LinkedHashSet<>(
                membershipRepository.findActiveStaffUserIdsByCondominiumId(event.condominiumId())
        );
        if (event.recipientId() != null) {
            recipients.add(event.recipientId());
        }
        realtimeEmitterService.sendToUsers(recipients, updateEvent);

        if (event.newStatus() != PackageStatus.NOTIFIED) {
            return;
        }

        if (!featureAccessService.hasFeature(
                event.condominiumId(),
                FeatureCode.NOTIFICATIONS
        )) {
            return;
        }

        notificationService.createPackageNotification(
                event.recipientId(),
                event.packageId()
        );
    }
}
