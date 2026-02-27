package com.cefet_rj.projeto_final.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "tb_condominium_user")
@Entity
public class CondominiumUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "condominium_id")
    private Condominium condominium;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private Role role;
}
