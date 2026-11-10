package com.cefet_rj.projeto_final.community.repositories;

import com.cefet_rj.projeto_final.community.entities.UtilityReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {
    @EntityGraph(attributePaths = {"unit", "unit.sector", "reader"})
    List<UtilityReading> findByCondominiumIdOrderByYearDescMonthDesc(Long condominiumId);

    @EntityGraph(attributePaths = {"unit", "unit.sector", "reader"})
    List<UtilityReading> findByCondominiumIdAndYearAndMonthOrderByUnitIdAsc(Long condominiumId, Integer year, Integer month);

    @EntityGraph(attributePaths = {"unit", "unit.sector", "reader"})
    List<UtilityReading> findByUnitIdOrderByYearDescMonthDesc(Long unitId);

    @EntityGraph(attributePaths = {"unit", "unit.sector", "reader"})
    Optional<UtilityReading> findByUnitIdAndYearAndMonthAndUtilityType(Long unitId, Integer year, Integer month, String utilityType);
}
