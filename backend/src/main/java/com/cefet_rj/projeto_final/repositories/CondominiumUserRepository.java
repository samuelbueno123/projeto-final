package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.CondominiumUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondominiumUserRepository extends JpaRepository<CondominiumUser, Long> {
}
