package com.cefet_rj.projeto_final.entities;

import com.cefet_rj.projeto_final.entities.enums.CondominiumBillingPlan;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tb_condominium")
public class Condominium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondominiumBillingPlan billingPlan;

    @Column(nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "condominium", fetch = FetchType.LAZY)
    private Set<Sector> sectors = new HashSet<>();
}
