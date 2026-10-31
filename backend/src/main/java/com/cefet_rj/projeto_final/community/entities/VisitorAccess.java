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
@Table(name = "tb_visitor_access")
public class VisitorAccess {

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

    @Column(name = "visitor_name", nullable = false, length = 140)
    private String visitorName;

    @Column(name = "document_number", length = 40)
    private String documentNumber;

    @Column(length = 30)
    private String phone;

    @Column(name = "visitor_type", nullable = false, length = 40)
    private String visitorType = "VISITOR";

    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate;

    @Column(name = "access_code", nullable = false, length = 20)
    private String accessCode;

    @Column(name = "expected_at")
    private LocalDateTime expectedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(nullable = false, length = 30)
    private String status = "AUTHORIZED";

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
