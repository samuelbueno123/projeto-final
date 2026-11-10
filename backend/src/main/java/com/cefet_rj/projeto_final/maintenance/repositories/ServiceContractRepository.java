package com.cefet_rj.projeto_final.maintenance.repositories;

import com.cefet_rj.projeto_final.maintenance.entities.ServiceContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceContractRepository extends JpaRepository<ServiceContract, Long> {
    List<ServiceContract> findByCondominiumIdOrderByEndDateAsc(Long condominiumId);
    List<ServiceContract> findByCondominiumIdAndStatusOrderByEndDateAsc(Long condominiumId, String status);
    List<ServiceContract> findByCondominiumIdAndEndDateLessThanEqualAndStatus(Long condominiumId, LocalDate maxDate, String status);
}
