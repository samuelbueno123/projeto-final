package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.GuestEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestEntryRepository extends JpaRepository<GuestEntry, Long> {
}
