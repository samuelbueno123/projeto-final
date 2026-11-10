package com.cefet_rj.projeto_final.users.repositories;

import com.cefet_rj.projeto_final.users.entities.Role;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

	@Cacheable(value = "roles", key = "#role", unless = "#result == null")
	Role findByAuthority(String role);

	@Override
	@CacheEvict(value = "roles", allEntries = true)
	<S extends Role> S save(S entity);

	@Override
	@CacheEvict(value = "roles", allEntries = true)
	<S extends Role> S saveAndFlush(S entity);

	@Override
	@CacheEvict(value = "roles", allEntries = true)
	void deleteAll();

	@Override
	@CacheEvict(value = "roles", allEntries = true)
	void delete(Role entity);

	@Override
	@CacheEvict(value = "roles", allEntries = true)
	void deleteById(Long id);
}

