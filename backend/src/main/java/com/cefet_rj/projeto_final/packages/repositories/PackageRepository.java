package com.cefet_rj.projeto_final.packages.repositories;

import com.cefet_rj.projeto_final.packages.entities.Package;
import com.cefet_rj.projeto_final.packages.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface PackageRepository extends JpaRepository<Package, Long> {

    @EntityGraph(attributePaths = {"recipient", "unit", "unit.sector", "unit.sector.condominium"})
    @NonNull
    Optional<Package> findById(@NonNull Long id);

    @Query(
            value = """
        SELECT p
        FROM Package p
        JOIN FETCH p.recipient
        JOIN FETCH p.unit u
        JOIN FETCH u.sector s
        JOIN FETCH s.condominium
        WHERE s.condominium.id = :condominiumId
    """,
            countQuery = """
        SELECT COUNT(p)
        FROM Package p
        JOIN p.unit u
        JOIN u.sector s
        WHERE s.condominium.id = :condominiumId
    """
    )
    Page<Package> findByCondominiumId(Long condominiumId, Pageable pageable);

    @Query(
            value = """
        SELECT p
        FROM Package p
        JOIN FETCH p.recipient
        JOIN FETCH p.unit u
        JOIN FETCH u.sector s
        JOIN FETCH s.condominium
        WHERE s.condominium.id = :condominiumId
          AND (
                LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(p.recipient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(p.recipient.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(u.identifier) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (:status IS NULL OR p.status = :status)
    """,
            countQuery = """
        SELECT COUNT(p)
        FROM Package p
        JOIN p.recipient r
        JOIN p.unit u
        JOIN u.sector s
        WHERE s.condominium.id = :condominiumId
          AND (
                LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(r.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(r.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(u.identifier) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
          )
          AND (:status IS NULL OR p.status = :status)
    """
    )
    Page<Package> searchPackagesByCondominium(
            Long condominiumId,
            String search,
            PackageStatus status,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT p
        FROM Package p
        JOIN FETCH p.recipient
        JOIN FETCH p.unit u
        JOIN FETCH u.sector s
        JOIN FETCH s.condominium
        WHERE p.recipient.id = :userId
          AND (:condominiumId IS NULL OR s.condominium.id = :condominiumId)
    """,
            countQuery = """
        SELECT COUNT(p)
        FROM Package p
        JOIN p.unit u
        JOIN u.sector s
        WHERE p.recipient.id = :userId
          AND (:condominiumId IS NULL OR s.condominium.id = :condominiumId)
    """
    )
    Page<Package> findByRecipientIdAndCondominiumId(Long userId, Long condominiumId, Pageable pageable);

    @Query(
            value = """
        SELECT p
        FROM Package p
        JOIN FETCH p.recipient
        JOIN FETCH p.unit u
        JOIN FETCH u.sector s
        JOIN FETCH s.condominium
        WHERE (p.recipient.id = :userId OR u.id IN :unitIds)
          AND (:condominiumId IS NULL OR s.condominium.id = :condominiumId)
    """,
            countQuery = """
        SELECT COUNT(p)
        FROM Package p
        JOIN p.unit u
        JOIN u.sector s
        WHERE (p.recipient.id = :userId OR u.id IN :unitIds)
          AND (:condominiumId IS NULL OR s.condominium.id = :condominiumId)
    """
    )
    Page<Package> findByRecipientIdOrUnitIdsAndCondominiumId(
            Long userId,
            List<Long> unitIds,
            Long condominiumId,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT p
        FROM Package p
        JOIN FETCH p.recipient
        JOIN FETCH p.unit u
        JOIN FETCH u.sector s
        JOIN FETCH s.condominium
        WHERE p.recipient.id = :userId
    """,
            countQuery = """
        SELECT COUNT(p)
        FROM Package p
        WHERE p.recipient.id = :userId
    """
    )
    Page<Package> findByRecipientId(Long userId, Pageable pageable);

    @Query("""
        select count(p)
        from Package p
        join p.unit u
        join u.sector s
        where s.condominium.id = :condominiumId
        """)
    long countByCondominiumId(
            Long condominiumId
    );

    @Query("""
        select p.status, count(p)
        from Package p
        join p.unit u
        join u.sector s
        where s.condominium.id = :condominiumId
        group by p.status
        """)
    List<Object[]> countByStatusForCondominium(
            Long condominiumId
    );
}