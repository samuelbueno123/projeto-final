package com.cefet_rj.projeto_final.maintenance.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "tb_maintenance_checklist_item")
public class MaintenanceChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private MaintenanceChecklist checklist;

    @Column(name = "item_description", nullable = false, length = 250)
    private String itemDescription;

    @Column(name = "area_or_location", length = 150)
    private String areaOrLocation;

    @Column(nullable = false, length = 30)
    private String status = "CONFORMING"; // CONFORMING, ATTENTION_NEEDED, NON_CONFORMING

    @Column(length = 500)
    private String observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private MaintenanceEquipment equipment;
}
