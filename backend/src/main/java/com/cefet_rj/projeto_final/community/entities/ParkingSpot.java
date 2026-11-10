package com.cefet_rj.projeto_final.community.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "tb_parking_spot",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_parking_spot_condo_code", columnNames = {"condominium_id", "code"})
        }
)
public class ParkingSpot {

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

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "spot_type", nullable = false, length = 30)
    private String spotType = "CAR";

    @Column(name = "location_description", length = 150)
    private String locationDescription;

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
