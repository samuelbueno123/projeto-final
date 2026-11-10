package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    @EntityGraph(attributePaths = {"user", "unit", "unit.sector", "unit.sector.condominium"})
    @NonNull
    Page<Resident> findAll(@NonNull Pageable pageable);

    @Query("""
            select r.unit.id, count(r)
            from Resident r
            where r.unit.sector.condominium.id = :condominiumId
            group by r.unit.id
            """)
    List<Object[]> countResidentsByUnitForCondominium(@Param("condominiumId") Long condominiumId);

    Optional<Resident> findFirstByUnitId(Long unitId);

    Optional<Resident> findByUserIdAndUnitId(Long userId, Long unitId);

    boolean existsByUserIdAndUnitId(Long userId, Long unitId);

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

    @Query("""
            select r
            from Resident r
            join fetch r.user u
            join fetch r.unit u2
            join fetch u2.sector s
            where s.condominium.id = :condominiumId
            """)
    List<Resident> findByCondominiumId(@Param("condominiumId") Long condominiumId);

    @Query("""
            select r
            from Resident r
            join fetch r.unit u
            join fetch u.sector s
            where r.user.id = :userId and s.condominium.id = :condominiumId
            """)
    List<Resident> findByUserIdAndCondominiumId(@Param("userId") Long userId, @Param("condominiumId") Long condominiumId);
}
