package com.cefet_rj.projeto_final.community.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_emergency_alert")
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User resident;

    @Column(name = "emergency_type", nullable = false, length = 40)
    private String emergencyType; // MEDICAL, FIRE, GAS_LEAK, ELEVATOR, SECURITY_BREACH, OTHER

    @Column(length = 1000)
    private String details;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, IN_ATTENDANCE, RESOLVED, FALSE_ALARM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attended_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User attendedBy;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
