package com.cefet_rj.projeto_final.entities;

import com.cefet_rj.projeto_final.entities.enums.SectorStructuralType;
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
@Table(name = "tb_sector",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"condominium_id", "name"}
                )
        }
)
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectorStructuralType structuralType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @OneToMany(mappedBy = "sector", fetch = FetchType.LAZY)
    private Set<Unit> units = new HashSet<>();
}
