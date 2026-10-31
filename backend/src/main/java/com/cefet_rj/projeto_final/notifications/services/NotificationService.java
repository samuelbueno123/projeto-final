package com.cefet_rj.projeto_final.notifications.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.notifications.dtos.SystemAnnouncementRequestDTO;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.packages.repositories.PackageRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Transactional
    public int publishSystemAnnouncement(SystemAnnouncementRequestDTO request) {
        String image = request.imageUrl();
        if (image != null && !image.isBlank()
                && !(image.startsWith("data:image/png;base64,")
                || image.startsWith("data:image/jpeg;base64,")
                || image.startsWith("data:image/webp;base64,"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Imagem deve ser PNG, JPEG ou WEBP em formato data URL.");
        }
        List<Notification> notifications = userRepository.findAll().stream().map(user -> {
            Notification notice = new Notification();
            notice.setRecipient(user);
            notice.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
            notice.setTitle(request.title().trim());
            notice.setMessage(request.message().trim());
            notice.setImageUrl(image);
            notice.setRead(false);
            notice.setCreatedAt(LocalDateTime.now());
            return notice;
        }).toList();
        notificationRepository.saveAll(notifications);
        return notifications.size();
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
        notificationRepository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createPackageNotification(
            Long recipientId,
            Long packageId
    ) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + recipientId));

        Notification notification = new Notification();

        notification.setRecipient(recipient);
        notification.setType(NotificationType.PACKAGE_RECEIVED);
        notification.setTitle("Encomenda disponível");
        notification.setMessage(
                packageRepository.findById(packageId)
                        .map(p -> p.getDescription() == null || p.getDescription().isBlank()
                                ? "Sua encomenda está disponível na portaria."
                                : "Sua encomenda (" + p.getDescription().trim() + ") está disponível na portaria.")
                        .orElse("Sua encomenda está disponível na portaria.")
        );
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAnnouncementNotifications(
            List<Long> recipientIds,
            String announcementTitle,
            Long condominiumId,
            String condominiumName
    ) {

        if (recipientIds.isEmpty()) {
            return;
        }

        List<User> recipients =
                userRepository.findAllById(recipientIds);

        List<Notification> notifications =
                new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (User recipient : recipients) {

            Notification notification = new Notification();

            notification.setRecipient(recipient);
            notification.setType(NotificationType.ANNOUNCEMENT);
            notification.setCondominiumId(condominiumId);
            notification.setCondominiumName(condominiumName);
            notification.setTitle("Novo comunicado");
            notification.setMessage(
                    "Novo comunicado disponível: "
                            + announcementTitle
            );
            notification.setRead(false);
            notification.setCreatedAt(now);

            notifications.add(notification);
        }

        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> findMyNotifications() {
        User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(authenticatedUser.getId())
                .stream()
                .map(NotificationDTO::new)
                .toList();
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

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void createReservationNotification(
            Long recipientId,
            String commonAreaName,
            LocalDateTime startAt
    ) {

        User recipient =
                userRepository.findById(recipientId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found: "
                                                + recipientId
                                )
                        );

        Notification notification =
                new Notification();

        notification.setRecipient(recipient);

        notification.setType(
                NotificationType.RESERVATION
        );

        notification.setTitle(
                "Reserva registrada"
        );

        notification.setMessage(
                "Sua reserva de "
                        + commonAreaName
                        + " foi registrada para "
                        + startAt
        );

        notification.setRead(false);

        notification.setCreatedAt(
                LocalDateTime.now()
        );

        notificationRepository.save(
                notification
        );
    }
}
