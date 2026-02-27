package com.cefet_rj.projeto_final.entities;

import com.cefet_rj.projeto_final.entities.enums.ResidentType;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "tb_resident")
@Entity
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Enumerated(EnumType.STRING)
    private ResidentType type;
}
