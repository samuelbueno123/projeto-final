package com.cefet_rj.projeto_final.assemblies.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_assembly_participant", uniqueConstraints = {
        @UniqueConstraint(name = "uk_assembly_participant_user", columnNames = {"assembly_id", "user_id"})
})
public class AssemblyParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assembly_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CondominiumAssembly assembly;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Unit unit;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt = LocalDateTime.now();

    @Column(name = "proxy_info", length = 250)
    private String proxyInfo;
}
