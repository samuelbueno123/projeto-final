package com.cefet_rj.projeto_final.maintenance.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_supplier")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;

    @Column(name = "contact_name", length = 140)
    private String contactName;

    @Column(length = 40)
    private String phone;

    @Column(length = 140)
    private String email;

    @Column(name = "document_number", length = 40)
    private String documentNumber;

    @Column(name = "service_category", nullable = false, length = 60)
    private String serviceCategory = "GERAL";

    @Column(length = 250)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
