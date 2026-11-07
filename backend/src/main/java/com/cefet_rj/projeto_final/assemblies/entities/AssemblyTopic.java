package com.cefet_rj.projeto_final.assemblies.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tb_assembly_topic")
public class AssemblyTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assembly_id", nullable = false)
    private CondominiumAssembly assembly;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "voting_open", nullable = false)
    private boolean votingOpen = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(name = "in_favor_count", nullable = false)
    private int inFavorCount = 0;

    @Column(name = "against_count", nullable = false)
    private int againstCount = 0;

    @Column(name = "abstain_count", nullable = false)
    private int abstainCount = 0;

    @Column(name = "result_status", nullable = false, length = 30)
    private String resultStatus = "PENDING";

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssemblyVote> votes = new ArrayList<>();
}
