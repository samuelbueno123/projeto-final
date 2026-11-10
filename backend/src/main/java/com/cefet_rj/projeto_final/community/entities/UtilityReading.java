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
@Table(
    name = "tb_utility_reading",
    uniqueConstraints = @UniqueConstraint(name = "uk_utility_unit_year_month_type", columnNames = {"unit_id", "year", "month", "utility_type"})
)
public class UtilityReading {

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

    @Column(name = "utility_type", nullable = false, length = 30)
    private String utilityType; // WATER, GAS

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "current_reading", nullable = false, precision = 10, scale = 3)
    private BigDecimal currentReading;

    @Column(name = "previous_reading", nullable = false, precision = 10, scale = 3)
    private BigDecimal previousReading;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal consumption;

    @Column(name = "leak_suspicion", nullable = false)
    private Boolean leakSuspicion = false;

    @Column(name = "meter_photo_url")
    private String meterPhotoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User reader;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
