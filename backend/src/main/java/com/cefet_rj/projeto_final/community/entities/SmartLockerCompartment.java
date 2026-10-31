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
@Table(name = "tb_smart_locker_compartment")
public class SmartLockerCompartment {

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
    @JoinColumn(name = "recipient_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposited_by_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User depositedBy;

    @Column(name = "locker_name", nullable = false, length = 80)
    private String lockerName = "Armário Inteligente Bloco Central";

    @Column(name = "compartment_number", nullable = false, length = 20)
    private String compartmentNumber;

    @Column(name = "pin_code", nullable = false, length = 20)
    private String pinCode;

    @Column(length = 300)
    private String description;

    @Column(nullable = false, length = 30)
    private String status = "OCCUPIED";

    @Column(name = "deposited_at", nullable = false)
    private LocalDateTime depositedAt = LocalDateTime.now();

    @Column(name = "retrieved_at")
    private LocalDateTime retrievedAt;
}
