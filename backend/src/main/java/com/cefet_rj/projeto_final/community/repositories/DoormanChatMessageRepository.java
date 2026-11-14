package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.DoormanChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoormanChatMessageRepository extends JpaRepository<DoormanChatMessage, Long> {

    @Query("""
        SELECT m FROM DoormanChatMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.recipient
        LEFT JOIN FETCH m.unit u
        LEFT JOIN FETCH u.sector
        WHERE m.condominium.id = :condoId
          AND (m.sender.id = :userId OR m.recipient.id = :userId)
        ORDER BY m.sentAt ASC
    """)
    List<DoormanChatMessage> findResidentConversation(
            @Param("condoId") Long condoId,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT m FROM DoormanChatMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.recipient
        LEFT JOIN FETCH m.unit u
        LEFT JOIN FETCH u.sector
        WHERE m.condominium.id = :condoId
          AND (:targetUserId IS NULL OR m.sender.id = :targetUserId OR m.recipient.id = :targetUserId)
        ORDER BY m.sentAt ASC
    """)
    List<DoormanChatMessage> findDoormanConversation(
            @Param("condoId") Long condoId,
            @Param("targetUserId") Long targetUserId
    );

    @Query("""
        SELECT m FROM DoormanChatMessage m
        JOIN FETCH m.sender
        LEFT JOIN FETCH m.recipient
        LEFT JOIN FETCH m.unit u
        LEFT JOIN FETCH u.sector
        WHERE m.condominium.id = :condoId
        ORDER BY m.sentAt ASC
    """)
    List<DoormanChatMessage> findAllByCondominiumIdWithDetails(@Param("condoId") Long condoId);

    @Query("""
        SELECT COUNT(m) FROM DoormanChatMessage m
        WHERE m.condominium.id = :condoId
          AND m.recipient.id = :userId
          AND m.isRead = false
    """)
    long countUnreadMessages(@Param("condoId") Long condoId, @Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE DoormanChatMessage m
        SET m.isRead = true
        WHERE m.condominium.id = :condoId
          AND m.recipient.id = :userId
          AND m.isRead = false
    """)
    void markAsReadForUser(@Param("condoId") Long condoId, @Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE DoormanChatMessage m
        SET m.isRead = true
        WHERE m.condominium.id = :condoId
          AND (m.recipient.id = :userId OR (m.recipient IS NULL AND m.senderRole = 'RESIDENT'))
          AND m.isRead = false
    """)
    int markAllAsReadForStaff(@Param("condoId") Long condoId, @Param("userId") Long userId);

    @Modifying
    @Query("""
        UPDATE DoormanChatMessage m
        SET m.isRead = true
        WHERE m.condominium.id = :condoId
          AND m.sender.id = :senderId
          AND m.senderRole = 'RESIDENT'
          AND m.isRead = false
    """)
    int markConversationAsReadForStaff(@Param("condoId") Long condoId, @Param("senderId") Long senderId);
}
