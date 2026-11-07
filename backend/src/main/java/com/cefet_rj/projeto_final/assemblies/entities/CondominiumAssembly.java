package com.cefet_rj.projeto_final.assemblies.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.users.entities.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "tb_condominium_assembly")
public class CondominiumAssembly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String type = "ORDINARY";

    @Column(name = "meeting_date", nullable = false)
    private LocalDateTime meetingDate;

    @Column(name = "second_call_date")
    private LocalDateTime secondCallDate;

    @Column(name = "location_or_link", length = 500)
    private String locationOrLink;

    @Column(name = "call_notice_url", length = 500)
    private String callNoticeUrl;

    @Column(name = "minutes_url", length = 500)
    private String minutesUrl;

    @Column(nullable = false, length = 30)
    private String status = "SCHEDULED";

    @Column(name = "block_delinquent_units", nullable = false)
    private boolean blockDelinquentUnits = true;

    @Column(name = "one_vote_per_unit", nullable = false)
    private boolean oneVotePerUnit = true;

    @Column(name = "quorum_minimum_percent", nullable = false)
    private int quorumMinimumPercent = 50;

    @OneToMany(mappedBy = "assembly", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<AssemblyTopic> topics = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
