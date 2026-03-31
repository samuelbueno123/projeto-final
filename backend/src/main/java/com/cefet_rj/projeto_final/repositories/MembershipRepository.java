package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
}
