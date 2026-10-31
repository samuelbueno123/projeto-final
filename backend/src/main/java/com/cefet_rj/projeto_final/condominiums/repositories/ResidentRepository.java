package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    Optional<Resident> findFirstByUnitId(Long unitId);

    Optional<Resident> findFirstByUserIdAndUnitSectorCondominiumId(Long userId, Long condominiumId);

    @Query("""
            select r
            from Resident r
            join fetch r.user u
            where r.unit.id = :unitId
            order by u.firstName, u.lastName
            """)
    List<Resident> findAllByUnitIdWithUser(@Param("unitId") Long unitId);

    @Query("""
            select distinct r.user.id
            from Resident r
            where r.unit.id = :unitId
            """)
    List<Long> findUserIdsByUnitId(@Param("unitId") Long unitId);

    @Query("""
            select distinct r.unit.id
            from Resident r
            where r.user.id = :userId
            """)
    List<Long> findUnitIdsByUserId(@Param("userId") Long userId);
}
