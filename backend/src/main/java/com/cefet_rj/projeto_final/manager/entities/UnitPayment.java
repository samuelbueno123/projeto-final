package com.cefet_rj.projeto_final.manager.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "tb_unit_payment_status")
public class UnitPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "unit_id", nullable = false, unique = true) private Unit unit;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private UnitPaymentStatus status;
    @Column(name = "due_date") private LocalDate dueDate;
    private BigDecimal amount;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "updated_by_id", nullable = false) private User updatedBy;
}
