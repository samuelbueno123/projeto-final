package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.projections.PackageProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PackageRepository extends JpaRepository<Package, Long> {

    @Query(nativeQuery = true, value = """
    SELECT * FROM (
        SELECT DISTINCT tb_package.id
        FROM tb_package
        INNER JOIN tb_user ON tb_package.recipient_id = tb_user.id
        INNER JOIN tb_unit ON tb_package.unit_id = tb_unit.id
        INNER JOIN tb_sector ON tb_unit.sector_id = tb_sector.id
        WHERE (
            LOWER(tb_package.description) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_user.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_user.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_unit.identifier) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_sector.name) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:status IS NULL OR tb_package.status = :status)
    ) AS tb_result
    """,
            countQuery = """
    SELECT COUNT(*) FROM (
        SELECT DISTINCT tb_package.id
        FROM tb_package
        INNER JOIN tb_user ON tb_package.recipient_id = tb_user.id
        INNER JOIN tb_unit ON tb_package.unit_id = tb_unit.id
        INNER JOIN tb_sector ON tb_unit.sector_id = tb_sector.id
        WHERE (
            LOWER(tb_package.description) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_user.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_user.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_unit.identifier) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(tb_sector.name) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:status IS NULL OR tb_package.status = :status)
    ) AS tb_result
    """)
    Page<PackageProjection> searchPackages(String search, String status, Pageable pageable);

    @Query("""
        SELECT obj
        FROM Package obj
        JOIN FETCH obj.recipient
        JOIN FETCH obj.unit u
        JOIN FETCH u.sector
        WHERE obj.id IN :packageIds
        """)
    List<Package> searchPackagesWithRelations(List<Long> packageIds);
}