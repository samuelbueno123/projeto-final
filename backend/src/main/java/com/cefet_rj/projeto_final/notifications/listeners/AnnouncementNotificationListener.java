package com.cefet_rj.projeto_final.notifications.listeners;

import com.cefet_rj.projeto_final.announcements.events.AnnouncementCreatedEvent;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AnnouncementNotificationListener {

    private final NotificationService notificationService;
    private final MembershipRepository membershipRepository;
    private final ResidentRepository residentRepository;
    private final CondominiumFeatureAccessService featureAccessService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(AnnouncementCreatedEvent event) {

        if (!featureAccessService.hasFeature(
                event.condominiumId(),
                FeatureCode.NOTIFICATIONS
        )) {
            return;
        }

        List<Long> residentIds =
                membershipRepository
                        .findResidentUserIdsByCondominiumId(
                                event.condominiumId()
                        );

        if (event.unitId() != null) {
            List<Long> unitResidentIds =
                    residentRepository.findUserIdsByUnitId(event.unitId());

            residentIds = residentIds.stream()
                    .filter(unitResidentIds::contains)
                    .toList();
        }

        if (residentIds.isEmpty()) {
            return;
        }

        notificationService.createAnnouncementNotifications(
                residentIds,
                event.title(),
                event.condominiumId(),
                event.condominiumName()
        );
    }
}