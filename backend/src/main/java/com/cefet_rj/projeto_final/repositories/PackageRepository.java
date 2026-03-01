package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.Package;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageRepository extends JpaRepository<Package, Long> {
}
