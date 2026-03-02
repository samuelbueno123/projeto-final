package com.cefet_rj.projeto_final.entities;

import com.cefet_rj.projeto_final.entities.enums.PackageStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(name = "tb_package")
@Entity
public class Package {

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

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime arrivalAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private LocalDateTime pickedUpAt;

    @PrePersist
    public void prePersist() {
        arrivalAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        pickedUpAt = LocalDateTime.now();
    }
}
