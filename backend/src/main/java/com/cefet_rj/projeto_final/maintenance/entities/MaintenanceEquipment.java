package com.cefet_rj.projeto_final.maintenance.entities;

import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_maintenance_equipment")
public class MaintenanceEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Condominium condominium;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 60)
    private String category = "ELEVADORES";

    @Column(length = 150)
    private String location;

    @Column(length = 120)
    private String manufacturer;

    @Column(name = "model_number", length = 100)
    private String modelNumber;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "installed_at")
    private LocalDate installedAt;

    @Column(name = "recurrence_days", nullable = false)
    private Integer recurrenceDays = 90;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date", nullable = false)
    private LocalDate nextMaintenanceDate;

    @Column(name = "responsible_name", length = 140)
    private String responsibleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_supplier_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Supplier preferredSupplier;

    @Column(nullable = false, length = 30)
    private String status = "OPERATIONAL";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
