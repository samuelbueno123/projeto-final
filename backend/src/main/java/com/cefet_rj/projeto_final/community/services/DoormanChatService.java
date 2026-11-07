package com.cefet_rj.projeto_final.community.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.community.dtos.ChatConversationDTO;
import com.cefet_rj.projeto_final.community.dtos.ChatMessageDTO;
import com.cefet_rj.projeto_final.community.entities.DoormanChatMessage;
import com.cefet_rj.projeto_final.community.repositories.DoormanChatMessageRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DoormanChatService {

    private final DoormanChatMessageRepository chatMessageRepository;
    private final CondominiumRepository condominiumRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final MembershipRepository membershipRepository;
    private final CondominiumAccessService condominiumAccessService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final RealtimeEmitterService realtimeEmitterService;

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> findMessages(Long condominiumId, Long targetUserId) {
        condominiumAccessService.validateAccess(condominiumId);
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = condominiumAccessService.isStaffOrAdmin(currentUser, condominiumId);

        if (isStaff) {
            return chatMessageRepository.findDoormanConversation(condominiumId, targetUserId).stream()
                    .map(ChatMessageDTO::new)
                    .toList();
        }

        return chatMessageRepository.findResidentConversation(condominiumId, currentUser.getId()).stream()
                .map(ChatMessageDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationDTO> findConversations(Long condominiumId) {
        condominiumAccessService.validateStaffAccess(condominiumId);
        List<Resident> residents = residentRepository.findByCondominiumId(condominiumId);
        Map<Long, ChatConversationDTO> map = new LinkedHashMap<>();

        for (Resident r : residents) {
            User u = r.getUser();
            if (u == null) continue;
            Long uid = u.getId();
            if (map.containsKey(uid)) continue;

            String userName = (u.getFirstName() + " " + (u.getLastName() != null ? u.getLastName() : "")).trim();
            Long unitId = r.getUnit() != null ? r.getUnit().getId() : null;
            String unitLabel = "";
            if (r.getUnit() != null) {
                String sec = r.getUnit().getSector() != null ? r.getUnit().getSector().getName() : "";
                String num = r.getUnit().getIdentifier();
                unitLabel = (sec.isEmpty() ? "" : sec + " - ") + "Unidade " + num;
            }

            List<DoormanChatMessage> history = chatMessageRepository.findDoormanConversation(condominiumId, uid);
            String lastMsg = null;
            LocalDateTime lastMsgAt = null;
            long unread = 0;
            if (!history.isEmpty()) {
                DoormanChatMessage last = history.get(history.size() - 1);
                lastMsg = last.getMessage();
                lastMsgAt = last.getSentAt();
                unread = history.stream()
                        .filter(m -> Boolean.FALSE.equals(m.getIsRead()) && m.getSender() != null && m.getSender().getId().equals(uid))
                        .count();
            }

            map.put(uid, new ChatConversationDTO(uid, userName, unitId, unitLabel, lastMsg, lastMsgAt, unread));
        }

        return map.values().stream()
                .sorted((a, b) -> {
                    if (a.lastMessageAt() != null && b.lastMessageAt() != null) {
                        return b.lastMessageAt().compareTo(a.lastMessageAt());
                    }
                    if (a.lastMessageAt() != null) return -1;
                    if (b.lastMessageAt() != null) return 1;
                    return a.userName().compareToIgnoreCase(b.userName());
                })
                .toList();
    }

    @Transactional
    public ChatMessageDTO sendMessage(Long condominiumId, ChatMessageDTO.SendMessageDTO dto) {
        condominiumAccessService.validateAccess(condominiumId);
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        boolean isStaff = condominiumAccessService.isStaffOrAdmin(currentUser, condominiumId);

        DoormanChatMessage msg = new DoormanChatMessage();
        msg.setCondominium(condominium);
        msg.setSender(currentUser);
        msg.setMessage(dto.message().trim());
        msg.setSentAt(LocalDateTime.now());
        msg.setIsRead(false);

        User recipientUser = null;
        if (isStaff) {
            boolean isManager = condominiumAccessService.isManagerOrAdmin(currentUser, condominiumId);
            msg.setSenderRole(isManager ? "MANAGER" : "DOORMAN");
            if (dto.recipientId() != null) {
                recipientUser = userRepository.findById(dto.recipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Destinatário não encontrado: " + dto.recipientId()));
                msg.setRecipient(recipientUser);

                Notification notice = new Notification();
                notice.setRecipient(recipientUser);
                notice.setType(NotificationType.DOORMAN_CHAT);
                notice.setTitle("Nova mensagem da Portaria");
                String preview = dto.message().length() > 100
                        ? dto.message().substring(0, 97) + "..."
                        : dto.message();
                notice.setMessage(preview);
                notice.setRead(false);
                notice.setCreatedAt(LocalDateTime.now());
                notice.setCondominiumId(condominiumId);
                notice.setCondominiumName(condominium.getName());
                notificationRepository.save(notice);
            }
            if (dto.unitId() != null) {
                unitRepository.findById(dto.unitId())
                        .filter(u -> u.getSector().getCondominium().getId().equals(condominiumId))
                        .ifPresent(msg::setUnit);
            } else if (recipientUser != null) {
                Long rUnitId = findResidentUnitId(condominiumId, recipientUser.getId());
                if (rUnitId != null) {
                    unitRepository.findById(rUnitId).ifPresent(msg::setUnit);
                }
            }
        } else {
            msg.setSenderRole("RESIDENT");
            Long residentUnitId = findResidentUnitId(condominiumId, currentUser.getId());
            if (residentUnitId != null) {
                unitRepository.findById(residentUnitId).ifPresent(msg::setUnit);
            }
        }

        msg = chatMessageRepository.save(msg);
        ChatMessageDTO result = new ChatMessageDTO(msg);

        // Envia via SSE apenas para os participantes autorizados (destinatário, remetente e equipe ativa do condomínio)
        RealtimeEventDTO realtimeEvent = RealtimeEventDTO.of("CHAT_MESSAGE", condominiumId, result);
        if (recipientUser != null) {
            realtimeEmitterService.sendToUser(recipientUser.getId(), realtimeEvent);
        }
        realtimeEmitterService.sendToUser(currentUser.getId(), realtimeEvent);
        membershipRepository.findActiveAndPendingByCondominiumId(condominiumId).stream()
                .filter(m -> m.getUser() != null && m.getRole() != null
                        && ("ROLE_MANAGER".equals(m.getRole().getAuthority()) || "ROLE_DOORMAN".equals(m.getRole().getAuthority())))
                .forEach(m -> realtimeEmitterService.sendToUser(m.getUser().getId(), realtimeEvent));

        return result;
    }

    @Transactional
    public void markMessagesAsRead(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
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
