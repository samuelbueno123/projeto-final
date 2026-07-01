package com.cefet_rj.projeto_final.condominiums.repositories;

import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    boolean existsByUserIdAndCondominiumId(Long userId, Long condominiumId);
}
