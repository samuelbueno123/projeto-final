package com.cefet_rj.projeto_final.assemblies.repositories;

import com.cefet_rj.projeto_final.assemblies.entities.AssemblyTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssemblyTopicRepository extends JpaRepository<AssemblyTopic, Long> {

    @Query("SELECT t FROM AssemblyTopic t LEFT JOIN FETCH t.assembly a WHERE t.id = :id")
    Optional<AssemblyTopic> findByIdWithAssembly(@Param("id") Long id);

    List<AssemblyTopic> findByAssemblyIdOrderByDisplayOrderAsc(Long assemblyId);
}
