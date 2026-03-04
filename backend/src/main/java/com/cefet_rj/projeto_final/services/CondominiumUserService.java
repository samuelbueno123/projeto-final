package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.CondominiumUserDTO;
import com.cefet_rj.projeto_final.entities.CondominiumUser;
import com.cefet_rj.projeto_final.repositories.CondominiumUserRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CondominiumUserService {

	private final CondominiumUserRepository condominiumUserRepository;

	@Transactional(readOnly = true)
	public Page<CondominiumUserDTO> findAllPaged(Pageable pageable) {
		Page<CondominiumUser> list = condominiumUserRepository.findAll(pageable);
		return list.map(CondominiumUserDTO::new);
	}

	@Transactional(readOnly = true)
	public CondominiumUserDTO findById(Long id) {
		Optional <CondominiumUser> obj = condominiumUserRepository.findById(id);
		CondominiumUser entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new CondominiumUserDTO(entity);
	}

}
