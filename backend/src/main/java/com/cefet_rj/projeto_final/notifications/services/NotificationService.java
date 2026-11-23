package com.cefet_rj.projeto_final.notifications.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.notifications.dtos.SystemAnnouncementRequestDTO;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final RealtimeEmitterService realtimeEmitterService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService condominiumAccessService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.users.services.PictureStorageService pictureStorageService;

    @Transactional
    public int publishSystemAnnouncement(SystemAnnouncementRequestDTO request) {
        String rawImage = request.imageUrl();
        String storedImageUrl = null;
        if (rawImage != null && !rawImage.isBlank()) {
            if (!(rawImage.startsWith("data:image/png;base64,")
                    || rawImage.startsWith("data:image/jpeg;base64,")
                    || rawImage.startsWith("data:image/webp;base64,"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Imagem deve ser PNG, JPEG ou WEBP em formato data URL.");
            }
            storedImageUrl = pictureStorageService != null
                    ? pictureStorageService.storeSystemAnnouncementImage(rawImage)
                    : rawImage;
        }
        final String imageForNotification = storedImageUrl;
        LocalDateTime now = LocalDateTime.now();
        String title = request.title().trim();
        String message = request.message().trim();

        int totalCreated = 0;
        int page = 0;
        final int batchSize = 200;
        while (true) {
            org.springframework.data.domain.Page<User> userPage =
                    userRepository.findAll(org.springframework.data.domain.PageRequest.of(page, batchSize));
            if (userPage.isEmpty()) {
                break;
            }
            List<Notification> batch = userPage.getContent().stream().map(user -> {
                Notification notice = new Notification();
                notice.setRecipient(user);
                notice.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
                notice.setTitle(title);
                notice.setMessage(message);
                notice.setImageUrl(imageForNotification);
                notice.setRead(false);
                notice.setCreatedAt(now);
                return notice;
            }).toList();
            notificationRepository.saveAll(batch);
            totalCreated += batch.size();
            if (!userPage.hasNext()) {
                break;
            }
            page++;
        }

        // Dispara evento em tempo real apenas após commit da transação
        dispatchAfterCommit(() -> realtimeEmitterService.sendToAll(
                RealtimeEventDTO.of("SYSTEM_ANNOUNCEMENT", null, Map.of(
                        "title", title,
                        "message", message,
                        "imageUrl", imageForNotification != null ? imageForNotification : ""
                ))));

        return totalCreated;
    }

    @Transactional
    public void createDirectNotification(
            User recipient,
            NotificationType type,
            String title,
            String message,
            Long condominiumId,
            String condominiumName
    ) {
        if (recipient == null) return;
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setCondominiumId(condominiumId);
        notification.setCondominiumName(condominiumName);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification = notificationRepository.save(notification);

        Long recipientId = recipient.getId();
        NotificationDTO dto = new NotificationDTO(notification);
        dispatchAfterCommit(() -> realtimeEmitterService.sendToUser(
                recipientId, RealtimeEventDTO.of("NOTIFICATION", condominiumId, dto)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPackageNotification(
            Long recipientId,
            Long packageId
    ) {
        createPackageNotification(recipientId, packageId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPackageNotification(
            Long recipientId,
            Long packageId,
            Long condominiumId
    ) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + recipientId));

        Notification notification = new Notification();

        var pkgOpt = packageRepository.findById(packageId);
        Long resolvedCondoId = condominiumId;
        String resolvedCondoName = null;
        if (pkgOpt.isPresent() && pkgOpt.get().getUnit() != null
                && pkgOpt.get().getUnit().getSector() != null
                && pkgOpt.get().getUnit().getSector().getCondominium() != null) {
            var condo = pkgOpt.get().getUnit().getSector().getCondominium();
            if (resolvedCondoId == null) {
                resolvedCondoId = condo.getId();
            }
            resolvedCondoName = condo.getName();
        }

        notification.setRecipient(recipient);
        notification.setType(NotificationType.PACKAGE_RECEIVED);
        notification.setCondominiumId(resolvedCondoId);
        notification.setCondominiumName(resolvedCondoName);
        notification.setTitle("Encomenda disponível");
        notification.setMessage(
                pkgOpt
                        .map(p -> p.getDescription() == null || p.getDescription().isBlank()
                                ? "Sua encomenda está disponível na portaria."
                                : "Sua encomenda (" + p.getDescription().trim() + ") está disponível na portaria.")
                        .orElse("Sua encomenda está disponível na portaria.")
        );
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);

        // Dispara evento em tempo real apenas após commit da transação
        NotificationDTO dto = new NotificationDTO(notification);
        Long finalCondoId = resolvedCondoId;
        dispatchAfterCommit(() -> {
            realtimeEmitterService.sendToUser(recipientId, RealtimeEventDTO.of("NOTIFICATION", finalCondoId, dto));
            realtimeEmitterService.sendToUser(recipientId, RealtimeEventDTO.of("PACKAGE_UPDATED", finalCondoId, dto));
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAnnouncementNotifications(
            List<Long> recipientIds,
            String announcementTitle,
            Long condominiumId,
            String condominiumName
    ) {
        saveAndDispatchAnnouncementNotifications(recipientIds, announcementTitle, condominiumId, condominiumName);
    }

    /**
     * Variante síncrona (Propagation.REQUIRED) para chamadas internas dentro de uma transação já aberta,
     * evitando esgotamento de pool por dupla alocação de conexão (REQUIRES_NEW aninhado).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void createAnnouncementNotificationsRequired(
            List<Long> recipientIds,
            String announcementTitle,
            Long condominiumId,
            String condominiumName
    ) {
        saveAndDispatchAnnouncementNotifications(recipientIds, announcementTitle, condominiumId, condominiumName);
    }

    private void saveAndDispatchAnnouncementNotifications(
            List<Long> recipientIds,
            String announcementTitle,
            Long condominiumId,
            String condominiumName
    ) {
        if (condominiumId == null) {
            throw new IllegalArgumentException("Notificações de aviso de condomínio exigem um único condominiumId.");
        }
        if (recipientIds.isEmpty()) {
            return;
        }

        List<User> recipients = userRepository.findAllById(recipientIds);
        List<Notification> notifications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setType(NotificationType.ANNOUNCEMENT);
            notification.setCondominiumId(condominiumId);
            notification.setCondominiumName(condominiumName);
            notification.setTitle("Novo comunicado");
            notification.setMessage("Novo comunicado disponível: " + announcementTitle);
            notification.setRead(false);
            notification.setCreatedAt(now);

            notifications.add(notification);
        }

        notificationRepository.saveAll(notifications);

        List<Map.Entry<Long, NotificationDTO>> prepared = notifications.stream()
                .filter(n -> n.getRecipient() != null)
                .map(n -> Map.entry(n.getRecipient().getId(), new NotificationDTO(n)))
                .toList();
        dispatchAfterCommit(() -> {
            // Dispara aviso em tempo real apenas aos destinatários autorizados (evita vazar comunicados segmentados)
            for (var entry : prepared) {
                Long uid = entry.getKey();
                NotificationDTO dto = entry.getValue();
                realtimeEmitterService.sendToUser(uid,
                        RealtimeEventDTO.of("NOTIFICATION", condominiumId, dto));
                realtimeEmitterService.sendToUser(uid,
                        RealtimeEventDTO.of("ANNOUNCEMENT", condominiumId, Map.of("title", announcementTitle)));
            }
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> findMyNotifications() {
        return findMyNotifications(null);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> findMyNotifications(Long condominiumId) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        if (condominiumId != null) {
            if (condominiumAccessService != null && !authenticatedUser.hasRole("ROLE_ADMIN")) {
                condominiumAccessService.validateUserBelongsToCondominium(
                        authenticatedUser.getId(),
                        condominiumId
                );
            }
            return notificationRepository
                    .findByRecipientIdAndCondominiumScopeOrderByCreatedAtDesc(
                            authenticatedUser.getId(),
                            condominiumId
                    )
                    .stream()
                    .map(NotificationDTO::new)
                    .toList();
        }

        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(authenticatedUser.getId())
                .stream()
                .map(NotificationDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications() {
        return countUnreadNotifications(null);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long condominiumId) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        if (condominiumId != null) {
            if (condominiumAccessService != null && !authenticatedUser.hasRole("ROLE_ADMIN")) {
                condominiumAccessService.validateUserBelongsToCondominium(
                        authenticatedUser.getId(),
                        condominiumId
                );
            }
            return notificationRepository.countUnreadByRecipientIdAndCondominiumScope(
                    authenticatedUser.getId(),
                    condominiumId
            );
        }
        return notificationRepository.countByRecipientIdAndReadFalse(authenticatedUser.getId());
    }

    @Transactional
    public void markAllAsRead() {
        markAllAsRead(null);
    }

    @Transactional
    public void markAllAsRead(Long condominiumId) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();
        if (condominiumId != null) {
            if (condominiumAccessService != null && !authenticatedUser.hasRole("ROLE_ADMIN")) {
                condominiumAccessService.validateUserBelongsToCondominium(
                        authenticatedUser.getId(),
                        condominiumId
                );
            }
            notificationRepository.markAllAsReadForUserInCondominiumScope(
                    authenticatedUser.getId(),
                    condominiumId
            );
            return;
        }
        notificationRepository.markAllAsReadForUser(authenticatedUser.getId());
    }

    @Transactional
    public NotificationDTO markAsRead(Long notificationId) {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        Notification notification = notificationRepository
                .findByIdAndRecipientId(
                        notificationId,
                        authenticatedUser.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Notification not found: " + notificationId
                        )
                );

        notification.setRead(true);

        return new NotificationDTO(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createReservationNotification(
            Long recipientId,
            String commonAreaName,
            LocalDateTime startAt
    ) {
        createReservationNotification(recipientId, commonAreaName, startAt, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createReservationNotification(
            Long recipientId,
            String commonAreaName,
            LocalDateTime startAt,
            Long condominiumId
    ) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + recipientId
                        ));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.RESERVATION);
        notification.setCondominiumId(condominiumId);
        notification.setTitle("Reserva registrada");
        notification.setMessage(
                "Sua reserva de " + commonAreaName + " foi registrada para " + startAt
        );
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);

        // Dispara evento em tempo real apenas após commit da transação
        NotificationDTO dto = new NotificationDTO(notification);
        dispatchAfterCommit(() -> {
            realtimeEmitterService.sendToUser(recipientId, RealtimeEventDTO.of("NOTIFICATION", condominiumId, dto));
            realtimeEmitterService.sendToUser(recipientId, RealtimeEventDTO.of("RESERVATION_UPDATED", condominiumId, dto));
        });
    }

    private void dispatchAfterCommit(Runnable action) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()
                && org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
        } else {
            action.run();
        }
    }
}
