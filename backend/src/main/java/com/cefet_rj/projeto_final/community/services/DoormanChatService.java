package com.cefet_rj.projeto_final.community.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.community.dtos.ChatConversationDTO;
import com.cefet_rj.projeto_final.community.dtos.ChatMessageDTO;
import com.cefet_rj.projeto_final.community.entities.DoormanChatMessage;
import com.cefet_rj.projeto_final.community.repositories.DoormanChatMessageRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final NotificationService notificationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final RealtimeEmitterService realtimeEmitterService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService featureAccessService;

    private void ensureChatFeature(Long condominiumId) {
        if (featureAccessService != null) {
            featureAccessService.validateFeatureEnabled(
                    condominiumId,
                    com.cefet_rj.projeto_final.billing.enums.FeatureCode.DOORMAN_CHAT
            );
        }
    }

    @Transactional
    public List<ChatMessageDTO> findMessages(Long condominiumId, Long targetUserId) {
        condominiumAccessService.validateAccess(condominiumId);
        ensureChatFeature(condominiumId);
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = condominiumAccessService.isStaffOrAdmin(currentUser, condominiumId);

        if (isStaff) {
            if (targetUserId != null) {
                chatMessageRepository.markConversationAsReadForStaff(condominiumId, targetUserId);
            }
            return chatMessageRepository.findDoormanConversation(condominiumId, targetUserId).stream()
                    .map(ChatMessageDTO::new)
                    .toList();
        }

        List<Long> myUnitIds = residentRepository.findByUserIdAndCondominiumId(currentUser.getId(), condominiumId).stream()
                .map(r -> r.getUnit().getId())
                .distinct()
                .toList();

        List<DoormanChatMessage> messages = myUnitIds.isEmpty()
                ? chatMessageRepository.findResidentDirectConversation(condominiumId, currentUser.getId())
                : chatMessageRepository.findResidentConversationScoped(condominiumId, currentUser.getId(), myUnitIds);

        return messages.stream()
                .map(ChatMessageDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationDTO> findConversations(Long condominiumId) {
        condominiumAccessService.validateStaffAccess(condominiumId);
        ensureChatFeature(condominiumId);
        List<Resident> residents = residentRepository.findByCondominiumId(condominiumId);

        List<Long> latestIds = chatMessageRepository.findLatestMessageIdsPerPair(condominiumId).stream()
                .map(row -> row[2] != null ? ((Number) row[2]).longValue() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, DoormanChatMessage> lastMessageByResident = new HashMap<>();
        if (!latestIds.isEmpty()) {
            for (DoormanChatMessage m : chatMessageRepository.findAllByIdInWithParticipants(latestIds)) {
                if (m.getSender() != null) {
                    lastMessageByResident.put(m.getSender().getId(), m);
                }
                if (m.getRecipient() != null && (m.getSender() == null || !m.getRecipient().getId().equals(m.getSender().getId()))) {
                    lastMessageByResident.put(m.getRecipient().getId(), m);
                }
            }
        }

        Map<Long, Long> unreadBySender = new HashMap<>();
        for (Object[] row : chatMessageRepository.countUnreadBySenderForCondominium(condominiumId)) {
            if (row[0] != null && row[1] != null) {
                unreadBySender.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }

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

            DoormanChatMessage last = lastMessageByResident.get(uid);
            String lastMsg = last != null ? last.getMessage() : null;
            LocalDateTime lastMsgAt = last != null ? last.getSentAt() : null;
            long unread = unreadBySender.getOrDefault(uid, 0L);

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
        ensureChatFeature(condominiumId);
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
        Unit targetUnit = null;
        if (dto.unitId() != null) {
            targetUnit = unitRepository.findById(dto.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + dto.unitId()));
            if (targetUnit.getSector() == null || !targetUnit.getSector().getCondominium().getId().equals(condominiumId)) {
                throw new ForbiddenException("A unidade selecionada não pertence a este condomínio.");
            }
        }

        if (isStaff) {
            boolean isManager = condominiumAccessService.isManagerOrAdmin(currentUser, condominiumId);
            msg.setSenderRole(isManager ? "MANAGER" : "DOORMAN");
            if (dto.recipientId() != null) {
                recipientUser = userRepository.findById(dto.recipientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Destinatário não encontrado: " + dto.recipientId()));
                if (!recipientUser.hasRole("ROLE_ADMIN")) {
                    condominiumAccessService.validateUserBelongsToCondominium(recipientUser.getId(), condominiumId);
                }
                if (targetUnit != null) {
                    boolean belongsToUnit = residentRepository.findUserIdsByUnitId(targetUnit.getId())
                            .contains(recipientUser.getId());
                    if (!belongsToUnit) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "O morador informado não está vinculado à unidade selecionada.");
                    }
                }
                msg.setRecipient(recipientUser);

                String preview = dto.message().length() > 100
                        ? dto.message().substring(0, 97) + "..."
                        : dto.message();
                notificationService.createDirectNotification(
                        recipientUser,
                        NotificationType.DOORMAN_CHAT,
                        "Nova mensagem da Portaria",
                        preview,
                        condominiumId,
                        condominium.getName()
                );
            } else if (targetUnit != null) {
                String preview = dto.message().length() > 100
                        ? dto.message().substring(0, 97) + "..."
                        : dto.message();
                for (Long residentUserId : residentRepository.findUserIdsByUnitId(targetUnit.getId())) {
                    userRepository.findById(residentUserId).ifPresent(unitUser ->
                            notificationService.createDirectNotification(
                                    unitUser,
                                    NotificationType.DOORMAN_CHAT,
                                    "Nova mensagem da Portaria",
                                    preview,
                                    condominiumId,
                                    condominium.getName()
                            )
                    );
                }
            }

            if (targetUnit != null) {
                msg.setUnit(targetUnit);
            } else if (recipientUser != null) {
                Long rUnitId = findResidentUnitId(condominiumId, recipientUser.getId());
                if (rUnitId != null) {
                    unitRepository.findById(rUnitId).ifPresent(msg::setUnit);
                }
            }
        } else {
            msg.setSenderRole("RESIDENT");
            if (targetUnit != null) {
                boolean ownsUnit = residentRepository.findByUserIdAndCondominiumId(currentUser.getId(), condominiumId).stream()
                        .anyMatch(r -> r.getUnit() != null && r.getUnit().getId().equals(dto.unitId()));
                if (!ownsUnit) {
                    throw new ForbiddenException("Você só pode enviar mensagens usando sua própria unidade.");
                }
                msg.setUnit(targetUnit);
            } else {
                Long residentUnitId = findResidentUnitId(condominiumId, currentUser.getId());
                if (residentUnitId != null) {
                    unitRepository.findById(residentUnitId).ifPresent(msg::setUnit);
                }
            }
        }

        msg = chatMessageRepository.save(msg);
        ChatMessageDTO result = new ChatMessageDTO(msg);

        // Envia via SSE apenas para a equipe ativa do condomínio e os moradores autorizados da unidade/destinatário
        RealtimeEventDTO realtimeEvent = RealtimeEventDTO.of("CHAT_MESSAGE", condominiumId, result);
        Set<Long> sseTargets = new LinkedHashSet<>(
                membershipRepository.findActiveStaffUserIdsByCondominiumId(condominiumId)
        );
        if (msg.getUnit() != null) {
            sseTargets.addAll(residentRepository.findUserIdsByUnitId(msg.getUnit().getId()));
        } else if (recipientUser != null) {
            sseTargets.add(recipientUser.getId());
        }
        sseTargets.add(currentUser.getId());
        realtimeEmitterService.sendToUsers(sseTargets, realtimeEvent);

        return result;
    }

    @Transactional
    public void markMessagesAsRead(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        ensureChatFeature(condominiumId);
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        if (condominiumAccessService.isStaffOrAdmin(currentUser, condominiumId)) {
            chatMessageRepository.markAllAsReadForStaff(condominiumId, currentUser.getId());
        } else {
            chatMessageRepository.markAsReadForUser(condominiumId, currentUser.getId());
        }
    }

    private Long findResidentUnitId(Long condominiumId, Long userId) {
        return residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(userId, condominiumId)
                .map(Resident::getUnit)
                .map(Unit::getId)
                .orElse(null);
    }
}
