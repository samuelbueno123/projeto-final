package com.cefet_rj.projeto_final.financial.repositories;

import com.cefet_rj.projeto_final.financial.entities.BudgetForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetForecastRepository extends JpaRepository<BudgetForecast, Long> {
    List<BudgetForecast> findByCondominiumIdAndYearAndMonth(Long condominiumId, Integer year, Integer month);
    Optional<BudgetForecast> findByCondominiumIdAndYearAndMonthAndCategory(Long condominiumId, Integer year, Integer month, String category);
}
