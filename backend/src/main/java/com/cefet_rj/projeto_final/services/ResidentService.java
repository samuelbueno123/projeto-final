package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.entities.Resident;
import com.cefet_rj.projeto_final.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;

	@Transactional(readOnly = true)
	public Page<ResidentDTO> findAllPaged(Pageable pageable) {
		Page<Resident> list = residentRepository.findAll(pageable);
	    return list.map(ResidentDTO::new);
	}

	@Transactional (readOnly = true)
	public ResidentDTO findById(Long id) {
		Optional <Resident> obj = residentRepository.findById(id);
		Resident entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new ResidentDTO(entity);
	}

}
