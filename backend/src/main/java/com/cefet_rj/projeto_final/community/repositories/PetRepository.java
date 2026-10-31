package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByCondominiumIdOrderByCreatedAtDesc(Long condominiumId);
    List<Pet> findByCondominiumIdAndOwnerIdOrderByCreatedAtDesc(Long condominiumId, Long ownerId);
}
