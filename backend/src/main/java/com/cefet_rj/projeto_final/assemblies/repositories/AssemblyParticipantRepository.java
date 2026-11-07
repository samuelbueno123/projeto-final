package com.cefet_rj.projeto_final.assemblies.repositories;

import com.cefet_rj.projeto_final.assemblies.entities.AssemblyParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssemblyParticipantRepository extends JpaRepository<AssemblyParticipant, Long> {
    List<AssemblyParticipant> findByAssemblyIdOrderByCheckedInAtAsc(Long assemblyId);
    Optional<AssemblyParticipant> findByAssemblyIdAndUserId(Long assemblyId, Long userId);
    long countByAssemblyId(Long assemblyId);
}
