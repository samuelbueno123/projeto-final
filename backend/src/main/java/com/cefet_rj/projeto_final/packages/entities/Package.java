package com.cefet_rj.projeto_final.packages.entities;

import com.cefet_rj.projeto_final.packages.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import com.cefet_rj.projeto_final.packages.factories.PackageStateFactory;
import com.cefet_rj.projeto_final.shared.projections.IdProjection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "tb_package")
@Entity
public class Package implements IdProjection<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unit_id")
    private Unit unit;

	@Enumerated(EnumType.STRING)
    private PackageStatus status;

	public void advanceStatus() {
		PackageStatusInterface state = PackageStateFactory.getState(this.status);
		state.raiseToNextState(this);
	}

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime receivedAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime notifiedAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime pickedUpAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime returnedAt;
}
