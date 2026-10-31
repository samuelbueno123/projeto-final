package com.cefet_rj.projeto_final.community.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_condominium_infraction")
public class CondominiumInfraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User issuedBy;

    @Column(nullable = false, length = 30)
    private String type = "WARNING";

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1500)
    private String description;

    @Column(name = "penalty_amount")
    private BigDecimal penaltyAmount;

    @Column(name = "defense_deadline")
    private LocalDate defenseDeadline;

    @Column(nullable = false, length = 30)
    private String status = "ISSUED";

    @Column(name = "resident_defense", length = 1500)
    private String residentDefense;

    @Column(name = "defense_submitted_at")
    private LocalDateTime defenseSubmittedAt;

    @Column(name = "manager_judgment", length = 1000)
    private String managerJudgment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
