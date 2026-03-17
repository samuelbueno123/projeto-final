package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.entities.Sector;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.entities.enums.CondominiumBillingPlan;
import com.cefet_rj.projeto_final.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.repositories.SectorRepository;
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

	private final SectorRepository sectorRepository;

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
	public CondominiumDTO update(Long id, CondominiumDTO dto){
		try {
			Condominium entity = condominiumRepository.getReferenceById(id);
			copyDtoToEntity(dto, entity);
			entity = condominiumRepository.save(entity);
			return new CondominiumDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional
	public CondominiumDTO insert(CondominiumDTO dto) {
		Condominium entity = new Condominium();
		copyDtoToEntity(dto, entity);
		entity = condominiumRepository.save(entity);
		return new CondominiumDTO(entity);
	}

	public void copyDtoToEntity(CondominiumDTO dto, Condominium entity) {
		entity.setName(dto.name());
		entity.setAddress(dto.address());
		entity.setBillingPlan(CondominiumBillingPlan.valueOf(dto.billingPlan()));
		entity.setIsActive(dto.isActive());

		entity.getSectors().clear();
		for (SectorDTO sectorDTO : dto.sectors()){
			Sector sector = sectorRepository.getReferenceById(sectorDTO.id());
			entity.getSectors().add(sector);
		}
	}
}
