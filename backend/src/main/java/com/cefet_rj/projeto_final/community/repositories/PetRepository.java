package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.Pet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    @EntityGraph(attributePaths = {"owner", "unit", "unit.sector"})
    List<Pet> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"owner", "unit", "unit.sector"})
    List<Pet> findByCondominiumIdAndOwnerIdOrderByCreatedAtDesc(Long condominiumId, Long ownerId);
}
