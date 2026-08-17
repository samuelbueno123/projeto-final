package com.cefet_rj.projeto_final.notifications.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.notifications.dtos.NotificationDTO;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

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
                "Sua encomenda #" + packageId + " está disponível na portaria."
        );
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
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
}