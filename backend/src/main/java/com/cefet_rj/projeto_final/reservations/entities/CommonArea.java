package com.cefet_rj.projeto_final.reservations.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tb_common_area")
public class CommonArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = true;

    @Column(length = 1000)
    private String rules;

    @Column(name = "opening_hour")
    private Integer openingHour = 8;

    @Column(name = "closing_hour")
    private Integer closingHour = 22;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;
}
