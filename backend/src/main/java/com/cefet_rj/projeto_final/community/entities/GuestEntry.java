package com.cefet_rj.projeto_final.community.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tb_guest_entry")
public class GuestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_list_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EventGuestList guestList;

    @Column(name = "guest_name", nullable = false, length = 140)
    private String guestName;

    @Column(name = "document_number", length = 40)
    private String documentNumber;

    @Column(nullable = false)
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
}
