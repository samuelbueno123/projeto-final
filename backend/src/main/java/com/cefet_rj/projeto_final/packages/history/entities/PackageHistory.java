package com.cefet_rj.projeto_final.packages.history.entities;

import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.history.enums.PackageHistoryActorType;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_package_history")
public class PackageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private Package packageEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private PackageStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private PackageStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false)
    private PackageHistoryActorType actorType;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 500)
    private String notes;
}