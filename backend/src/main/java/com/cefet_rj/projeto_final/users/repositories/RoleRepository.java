package com.cefet_rj.projeto_final.users.repositories;

import com.cefet_rj.projeto_final.users.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Role findByAuthority(String role);
}
