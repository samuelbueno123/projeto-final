package com.cefet_rj.projeto_final.repositories;

import com.cefet_rj.projeto_final.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
