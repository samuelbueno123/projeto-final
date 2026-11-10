package com.cefet_rj.projeto_final.maintenance.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
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
@Table(name = "tb_service_contract")
public class ServiceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Supplier supplier;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "contract_number", length = 80)
    private String contractNumber;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "monthly_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyValue = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;

    @Column(name = "renewal_notice_days", nullable = false)
    private Integer renewalNoticeDays = 30;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, EXPIRING_SOON, EXPIRED, TERMINATED

    @Column(name = "coverage_description", length = 1000)
    private String coverageDescription;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
