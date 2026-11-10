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
@Table(name = "tb_key_registry")
public class KeyRegistryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @Column(name = "key_code", nullable = false, length = 50)
    private String keyCode;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "key_category", nullable = false, length = 40)
    private String keyCategory = "COMMON_AREA"; // COMMON_AREA, TECHNICAL_ROOM, RESIDENT_BACKUP, GATE_TAG

    @Column(name = "current_status", nullable = false, length = 30)
    private String currentStatus = "AVAILABLE"; // AVAILABLE, BORROWED, LOST

    @Column(name = "borrower_name", length = 150)
    private String borrowerName;

    @Column(name = "borrower_document", length = 40)
    private String borrowerDocument;

    @Column(name = "borrower_phone", length = 40)
    private String borrowerPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Unit unit;

    @Column(name = "borrowed_at")
    private LocalDateTime borrowedAt;

    @Column(name = "expected_return_at")
    private LocalDateTime expectedReturnAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_out_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User checkedOutBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User checkedInBy;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
