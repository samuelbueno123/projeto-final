package com.cefet_rj.projeto_final.community.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_concierge_shift_log")
public class ConciergeShiftLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outgoing_doorman_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User outgoingDoorman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incoming_doorman_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User incomingDoorman;

    @Column(name = "shift_type", nullable = false, length = 30)
    private String shiftType; // MORNING, AFTERNOON, NIGHT

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "gate_status", nullable = false, length = 30)
    private String gateStatus = "ALL_NORMAL";

    @Column(name = "pending_keys_count", nullable = false)
    private Integer pendingKeysCount = 0;

    @Column(name = "pending_packages_count", nullable = false)
    private Integer pendingPackagesCount = 0;

    @Column(name = "occurrences_summary", length = 1500)
    private String occurrencesSummary;

    @Column(name = "general_notes", length = 1500)
    private String generalNotes;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
