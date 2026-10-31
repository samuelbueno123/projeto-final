package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("""
            select case when count(m) > 0 then true else false end
            from Membership m
            where m.user.id = :userId
              and m.condominium.id = :condominiumId
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            """)
    boolean existsByUserIdAndCondominiumId(
            @Param("userId") Long userId,
            @Param("condominiumId") Long condominiumId
    );

    @Query("""
            select m
            from Membership m
            join fetch m.user
            join fetch m.condominium
            join fetch m.role
            left join fetch m.requestedUnit ru
            left join fetch ru.sector
            where m.user.id = :userId
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            order by m.id asc
            """)
    List<Membership> findAllByUserId(@Param("userId") Long userId);

    @Query("""
            select m
            from Membership m
            join fetch m.user
            join fetch m.condominium
            join fetch m.role
            left join fetch m.requestedUnit ru
            left join fetch ru.sector
            where m.user.id = :userId
              and m.status in (
                  com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.PENDING,
                  com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.REQUESTED
              )
            order by m.createdAt desc
            """)
    List<Membership> findPendingAndRequestedByUserId(@Param("userId") Long userId);

    @Query("""
            select count(distinct m.user.id)
            from Membership m
            where m.condominium.id = :condominiumId
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            """)
    long countDistinctUsers(@Param("condominiumId") Long condominiumId);

    @Query("""
            select case when count(m) > 0 then true else false end
            from Membership m
            where m.user.id = :userId
              and m.condominium.id = :condominiumId
              and m.role.authority = 'ROLE_MANAGER'
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            """)
    boolean isManagerOf(@Param("userId") Long userId, @Param("condominiumId") Long condominiumId);

    @Query("""
            select m.user.id
            from Membership m
            where m.condominium.id = :condominiumId
              and m.role.authority = 'ROLE_RESIDENT'
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            """)
    List<Long> findResidentUserIdsByCondominiumId(
            @Param("condominiumId") Long condominiumId
    );

    @Query("""
            select m
            from Membership m
            join fetch m.user
            join fetch m.condominium
            join fetch m.role
            left join fetch m.requestedUnit ru
            left join fetch ru.sector
            where m.condominium.id = :condominiumId
              and m.status in (
                  com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE,
                  com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.PENDING,
                  com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.REQUESTED
              )
            order by m.id desc
            """)
    List<Membership> findActiveAndPendingByCondominiumId(@Param("condominiumId") Long condominiumId);

    @Query("""
            select m
            from Membership m
            join fetch m.user
            join fetch m.condominium
            join fetch m.role
            where m.condominium.id = :condominiumId
              and m.role.authority = 'ROLE_MANAGER'
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            order by m.id desc
            """)
    List<Membership> findActiveManagersByCondominiumId(@Param("condominiumId") Long condominiumId);

    @Query("""
            select m
            from Membership m
            join fetch m.user
            join fetch m.condominium
            join fetch m.role
            left join fetch m.requestedUnit ru
            left join fetch ru.sector
            where m.user.id = :userId
              and m.condominium.id = :condominiumId
              and m.role.authority = :authority
            order by m.id desc
            """)
    List<Membership> findByUserIdAndCondominiumIdAndAuthority(
            @Param("userId") Long userId,
            @Param("condominiumId") Long condominiumId,
            @Param("authority") String authority
    );

    @Query("""
            select m
            from Membership m
            join fetch m.user
            join fetch m.condominium
            join fetch m.role
            left join fetch m.requestedUnit ru
            left join fetch ru.sector
            where m.id = :id
            """)
    Optional<Membership> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            select case when count(m) > 0 then true else false end
            from Membership m
            where m.user.id = :userId
              and m.role.authority = :authority
              and m.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
            """)
    boolean existsActiveByUserIdAndAuthority(
            @Param("userId") Long userId,
            @Param("authority") String authority
    );

    @Query(
            value = """
                    select m
                    from Membership m
                    where m.condominium.id in (
                        select mgr.condominium.id
                        from Membership mgr
                        where mgr.user.id = :managerUserId
                          and mgr.role.authority = 'ROLE_MANAGER'
                          and mgr.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
                    )
                    """,
            countQuery = """
                    select count(m)
                    from Membership m
                    where m.condominium.id in (
                        select mgr.condominium.id
                        from Membership mgr
                        where mgr.user.id = :managerUserId
                          and mgr.role.authority = 'ROLE_MANAGER'
                          and mgr.status = com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus.ACTIVE
                    )
                    """
    )
    Page<Membership> findPagedForManager(
            @Param("managerUserId") Long managerUserId,
            Pageable pageable
    );
}
