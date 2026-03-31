package com.cefet_rj.projeto_final.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Setter;

@Data
@Table(name = "tb_condominium_user")
@Entity
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

	@Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "condominium_id")
    private Condominium condominium;

	@Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private Role role;
}
