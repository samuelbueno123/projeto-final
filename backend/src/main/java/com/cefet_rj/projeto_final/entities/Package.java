package com.cefet_rj.projeto_final.entities;

import com.cefet_rj.projeto_final.contracts.PackageStatusInterface;
import com.cefet_rj.projeto_final.entities.enums.PackageStatus;
import com.cefet_rj.projeto_final.factories.PackageStateFactory;
import com.cefet_rj.projeto_final.projections.IdProjection;
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
    private LocalDateTime arrivalAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime pickedUpAt;

    @PrePersist
    public void prePersist() {
		if (this.status == PackageStatus.PENDING) arrivalAt = LocalDateTime.now();
		else arrivalAt = null;
    }

    @PreUpdate
    public void preUpdate(){
		if (this.status == PackageStatus.PICKED_UP) pickedUpAt = LocalDateTime.now();
		else pickedUpAt = null;
    }
}
