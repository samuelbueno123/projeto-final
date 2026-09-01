package com.cefet_rj.projeto_final.notifications.listeners;

import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.events.PackageStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PackageNotificationListener {

    private final NotificationService notificationService;
    private final CondominiumFeatureAccessService featureAccessService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PackageStatusChangedEvent event) {

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