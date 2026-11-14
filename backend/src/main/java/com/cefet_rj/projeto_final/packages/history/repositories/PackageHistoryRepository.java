package com.cefet_rj.projeto_final.packages.history.repositories;

import com.cefet_rj.projeto_final.packages.history.entities.PackageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PackageHistoryRepository
        extends JpaRepository<PackageHistory, Long> {

    List<PackageHistory> findByPackageEntityIdOrderByPerformedAtAsc(Long packageId);

    @Modifying
    @Query("DELETE FROM PackageHistory ph WHERE ph.packageEntity.id = :packageId")
    void deleteByPackageEntityId(@Param("packageId") Long packageId);
}