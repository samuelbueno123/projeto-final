package com.cefet_rj.projeto_final.notifications.repositories;

import com.cefet_rj.projeto_final.notifications.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient.id = :recipientId
              AND (n.condominiumId = :condominiumId OR (n.condominiumId IS NULL AND n.type <> com.cefet_rj.projeto_final.notifications.enums.NotificationType.ANNOUNCEMENT))
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByRecipientIdAndCondominiumScopeOrderByCreatedAtDesc(
            @Param("recipientId") Long recipientId,
            @Param("condominiumId") Long condominiumId
    );

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    List<Notification> findByInvitationId(Long invitationId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.recipient.id = :recipientId
              AND n.read = false
              AND (n.condominiumId = :condominiumId OR (n.condominiumId IS NULL AND n.type <> com.cefet_rj.projeto_final.notifications.enums.NotificationType.ANNOUNCEMENT))
            """)
    long countUnreadByRecipientIdAndCondominiumScope(
            @Param("recipientId") Long recipientId,
            @Param("condominiumId") Long condominiumId
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    void markAllAsReadForUser(@Param("recipientId") Long recipientId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n SET n.read = true
            WHERE n.recipient.id = :recipientId
              AND n.read = false
              AND (n.condominiumId = :condominiumId OR (n.condominiumId IS NULL AND n.type <> com.cefet_rj.projeto_final.notifications.enums.NotificationType.ANNOUNCEMENT))
            """)
    void markAllAsReadForUserInCondominiumScope(
            @Param("recipientId") Long recipientId,
            @Param("condominiumId") Long condominiumId
    );
}
