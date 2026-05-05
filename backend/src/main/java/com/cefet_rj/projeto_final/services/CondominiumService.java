package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.dtos.insert.CondominiumInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.CondominiumUpdateDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CondominiumService {

    private final CondominiumRepository condominiumRepository;

    @Transactional(readOnly = true)
    public Page<CondominiumDTO> findAllPaged(Pageable pageable) {
        Page<Condominium> list = condominiumRepository.findAll(pageable);
        return list.map(CondominiumDTO::new);
    }

    @Transactional(readOnly = true)
    public CondominiumDTO findById(Long id) {
        Optional<Condominium> obj = condominiumRepository.findById(id);
        Condominium entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return new CondominiumDTO(entity);
    }

	@Transactional
	public CondominiumDTO insert(CondominiumInsertDTO dto) {
		Condominium entity = new Condominium();
		linkAssociations(dto.name(), dto.address(), dto.billingPlan(), dto.isActive(), entity);
		entity = condominiumRepository.save(entity);
		return new CondominiumDTO(entity);
	}

	@Transactional
	public CondominiumDTO update(Long id, CondominiumUpdateDTO dto){
		try {
			Condominium entity = condominiumRepository.getReferenceById(id);
			linkAssociations(dto.name(), dto.address(), dto.billingPlan(), dto.isActive(), entity);
			entity = condominiumRepository.save(entity);
			return new CondominiumDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	private void linkAssociations(String name, String address, com.cefet_rj.projeto_final.entities.enums.CondominiumBillingPlan billingPlan,
								  Boolean isActive, Condominium entity) {
		entity.setName(name);
		entity.setAddress(address);
		entity.setBillingPlan(billingPlan);
		entity.setIsActive(isActive);
	}
}
