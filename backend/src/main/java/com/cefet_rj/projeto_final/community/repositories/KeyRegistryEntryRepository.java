package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.KeyRegistryEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeyRegistryEntryRepository extends JpaRepository<KeyRegistryEntry, Long> {
    @EntityGraph(attributePaths = {"unit", "checkedOutBy", "checkedInBy"})
    List<KeyRegistryEntry> findByCondominiumIdOrderByKeyCodeAsc(Long condominiumId);

    @EntityGraph(attributePaths = {"unit", "checkedOutBy", "checkedInBy"})
    List<KeyRegistryEntry> findByCondominiumIdAndCurrentStatusOrderByKeyCodeAsc(Long condominiumId, String currentStatus);
}
