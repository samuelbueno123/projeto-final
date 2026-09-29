package com.cefet_rj.projeto_final.packages.history.repositories;

import com.cefet_rj.projeto_final.packages.history.entities.PackageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PackageHistoryRepository
        extends JpaRepository<PackageHistory, Long> {

    List<PackageHistory> findByPackageEntityIdOrderByPerformedAtAsc(Long packageId);
}