package com.cefet_rj.projeto_final.notifications.listeners;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PackageStatusChangedEvent event) {
        if (event.newStatus() != PackageStatus.NOTIFIED) {
            return;
        }

        notificationService.createPackageNotification(
                event.recipientId(),
                event.packageId()
        );
    }
}