package com.cefet_rj.projeto_final.maintenance.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tb_maintenance_checklist")
public class MaintenanceChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 30)
    private String frequency = "MONTHLY"; // DAILY, WEEKLY, MONTHLY, SEMIANNUAL, ANNUAL

    @Column(nullable = false, length = 30)
    private String status = "DRAFT"; // DRAFT, COMPLETED, REQUIRES_ACTION

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User inspector;

    @Column(name = "general_notes", length = 1000)
    private String generalNotes;

    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaintenanceChecklistItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
