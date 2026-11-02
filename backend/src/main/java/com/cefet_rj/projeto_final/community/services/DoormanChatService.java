package com.cefet_rj.projeto_final.community.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.community.dtos.ChatMessageDTO;
import com.cefet_rj.projeto_final.community.entities.DoormanChatMessage;
import com.cefet_rj.projeto_final.community.repositories.DoormanChatMessageRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoormanChatService {

    private final DoormanChatMessageRepository chatMessageRepository;
    private final CondominiumRepository condominiumRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> findMessages(Long condominiumId, Long targetUserId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = currentUser.hasRole("ROLE_ADMIN")
                || currentUser.hasRole("ROLE_MANAGER")
                || currentUser.hasRole("ROLE_DOORMAN");

        if (isStaff) {
            return chatMessageRepository.findDoormanConversation(condominiumId, targetUserId).stream()
                    .map(ChatMessageDTO::new)
                    .toList();
        }

        Long unitId = findResidentUnitId(condominiumId, currentUser.getId());
        return chatMessageRepository.findResidentConversation(condominiumId, currentUser.getId(), unitId).stream()
                .map(ChatMessageDTO::new)
                .toList();
    }

    @Transactional
    public ChatMessageDTO sendMessage(Long condominiumId, ChatMessageDTO.SendMessageDTO dto) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        boolean isStaff = currentUser.hasRole("ROLE_ADMIN")
                || currentUser.hasRole("ROLE_MANAGER")
                || currentUser.hasRole("ROLE_DOORMAN");

        DoormanChatMessage msg = new DoormanChatMessage();
        msg.setCondominium(condominium);
        msg.setSender(currentUser);
        msg.setMessage(dto.message().trim());
        msg.setSentAt(LocalDateTime.now());
        msg.setIsRead(false);

        if (isStaff) {
            msg.setSenderRole(currentUser.hasRole("ROLE_DOORMAN") ? "DOORMAN" : "MANAGER");
            if (dto.recipientId() != null) {
                User recipient = userRepository.findById(dto.recipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Destinatário não encontrado: " + dto.recipientId()));
                msg.setRecipient(recipient);

                // Dispara notificação no sistema para o morador
                Notification notice = new Notification();
                notice.setRecipient(recipient);
                notice.setType(NotificationType.DOORMAN_CHAT);
                notice.setTitle("Nova mensagem da Portaria");
                String preview = dto.message().length() > 100
                        ? dto.message().substring(0, 97) + "..."
                        : dto.message();
                notice.setMessage(preview);
                notice.setRead(false);
                notice.setCreatedAt(LocalDateTime.now());
                notificationRepository.save(notice);
            }
            if (dto.unitId() != null) {
                unitRepository.findById(dto.unitId()).ifPresent(msg::setUnit);
            }
        } else {
            msg.setSenderRole("RESIDENT");
            Long residentUnitId = findResidentUnitId(condominiumId, currentUser.getId());
            if (residentUnitId != null) {
                unitRepository.findById(residentUnitId).ifPresent(msg::setUnit);
            }
        }

        msg = chatMessageRepository.save(msg);
        return new ChatMessageDTO(msg);
    }

    @Transactional
    public void markMessagesAsRead(Long condominiumId) {
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        chatMessageRepository.markAsReadForUser(condominiumId, currentUser.getId());
    }

    private Long findResidentUnitId(Long condominiumId, Long userId) {
        return residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(userId, condominiumId)
                .map(Resident::getUnit)
                .map(Unit::getId)
                .orElse(null);
    }
}
