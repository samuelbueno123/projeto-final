package com.cefet_rj.projeto_final.assemblies.repositories;

import com.cefet_rj.projeto_final.assemblies.entities.CondominiumAssembly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CondominiumAssemblyRepository extends JpaRepository<CondominiumAssembly, Long> {

    @Query("SELECT DISTINCT a FROM CondominiumAssembly a " +
           "LEFT JOIN FETCH a.condominium c " +
           "LEFT JOIN FETCH a.createdBy u " +
           "LEFT JOIN FETCH a.topics t " +
           "WHERE a.condominium.id = :condominiumId " +
           "ORDER BY a.meetingDate DESC")
    List<CondominiumAssembly> findAllByCondominiumIdWithTopics(@Param("condominiumId") Long condominiumId);

    @Query("SELECT a FROM CondominiumAssembly a " +
           "LEFT JOIN FETCH a.condominium c " +
           "LEFT JOIN FETCH a.createdBy u " +
           "LEFT JOIN FETCH a.topics t " +
           "WHERE a.id = :id")
    Optional<CondominiumAssembly> findByIdWithTopics(@Param("id") Long id);
}
