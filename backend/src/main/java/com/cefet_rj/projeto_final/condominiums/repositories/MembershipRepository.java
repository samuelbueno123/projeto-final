package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    boolean existsByUserIdAndCondominiumId(
            Long userId,
            Long condominiumId
    );

    List<Membership> findAllByUserId(Long userId);

    @Query("select count(distinct m.user.id) from Membership m where m.condominium.id = :condominiumId")
    long countDistinctUsers(@Param("condominiumId") Long condominiumId);

    @Query("select case when count(m) > 0 then true else false end from Membership m where m.user.id = :userId and m.condominium.id = :condominiumId and m.role.authority = 'ROLE_MANAGER'")
    boolean isManagerOf(@Param("userId") Long userId, @Param("condominiumId") Long condominiumId);

    @Query("""
            select m.user.id
            from Membership m
            where m.condominium.id = :condominiumId
              and m.role.authority = 'ROLE_RESIDENT'
            """)
    List<Long> findResidentUserIdsByCondominiumId(
            @Param("condominiumId") Long condominiumId
    );
}
