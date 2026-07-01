package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.billing.enums.CondominiumBillingPlan;
import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.CondominiumInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.CondominiumUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!condominiumRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			condominiumRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	private void linkAssociations(
			String name,
			String address,
			String billingPlan,
			Boolean isActive,
			Condominium entity
	) {
		entity.setName(name);
		entity.setAddress(address);
		entity.setBillingPlan(CondominiumBillingPlan.fromString(billingPlan));
		entity.setIsActive(isActive != null ? isActive : true);
	}
}
