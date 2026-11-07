package com.cefet_rj.projeto_final.maintenance.repositories;

import com.cefet_rj.projeto_final.maintenance.entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByCondominiumIdOrderByCompanyNameAsc(Long condominiumId);
}
