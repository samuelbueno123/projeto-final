package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.KeyRegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeyRegistryEntryRepository extends JpaRepository<KeyRegistryEntry, Long> {
    List<KeyRegistryEntry> findByCondominiumIdOrderByKeyCodeAsc(Long condominiumId);
    List<KeyRegistryEntry> findByCondominiumIdAndCurrentStatusOrderByKeyCodeAsc(Long condominiumId, String currentStatus);
}
