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

        List<Long> recipientIds;
        if (event.targetRole() != null && !event.targetRole().isBlank()) {
            recipientIds = membershipRepository
                    .findActiveUserIdsByCondominiumIdAndAuthority(
                            event.condominiumId(),
                            event.targetRole()
                    );
        } else {
            recipientIds = membershipRepository
                    .findResidentUserIdsByCondominiumId(
                            event.condominiumId()
                    );
        }

        if (event.unitId() != null) {
            java.util.Set<Long> unitResidentIds = new java.util.HashSet<>(
                    residentRepository.findUserIdsByUnitId(event.unitId()));
            recipientIds = recipientIds.stream()
                    .filter(unitResidentIds::contains)
                    .toList();
        } else if (event.sectorId() != null) {
            java.util.Set<Long> sectorResidentIds = new java.util.HashSet<>(
                    residentRepository.findUserIdsBySectorId(event.sectorId()));
            recipientIds = recipientIds.stream()
                    .filter(sectorResidentIds::contains)
                    .toList();
        }

        if (recipientIds.isEmpty()) {
            return;
        }

        notificationService.createAnnouncementNotifications(
                recipientIds,
                event.title(),
                event.condominiumId(),
                event.condominiumName()
        );
    }
}