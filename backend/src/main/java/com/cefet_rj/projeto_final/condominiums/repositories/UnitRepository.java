package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    @Query("select u from Unit u join fetch u.sector s where s.condominium.id = :condominiumId order by s.name, u.identifier")
    List<Unit> findAllForCondominium(@Param("condominiumId") Long condominiumId);

    @Query("select count(u) from Unit u where u.sector.condominium.id = :condominiumId")
    long countForCondominium(@Param("condominiumId") Long condominiumId);
}
