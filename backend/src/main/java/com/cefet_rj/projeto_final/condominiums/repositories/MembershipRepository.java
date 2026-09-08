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