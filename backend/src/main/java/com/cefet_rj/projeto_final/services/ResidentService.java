package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.dtos.insert.ResidentInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.ResidentUpdateDTO;
import com.cefet_rj.projeto_final.entities.Resident;
import com.cefet_rj.projeto_final.entities.Unit;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.mapper.ResidentMapper;
import com.cefet_rj.projeto_final.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.repositories.UnitRepository;
import com.cefet_rj.projeto_final.repositories.UserRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;

	private final ResidentMapper residentMapper;

	private final UnitRepository unitRepository;

	private final UserRepository userRepository;

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

	@Transactional
	public ResidentDTO insert(ResidentInsertDTO dto) {
		Resident entity = new Resident();
		residentMapper.copyInsertDtoToEntity(dto, entity);
		linkAssociations(dto.unitId(), entity, dto.userId());
		entity = residentRepository.save(entity);
		return new ResidentDTO(entity);
	}

	@Transactional
	public ResidentDTO update(Long id, ResidentUpdateDTO dto){
		try {
			Resident entity = residentRepository.getReferenceById(id);
			residentMapper.copyUpdateDtoToEntity(dto, entity);
			linkAssociations(dto.unitId(), entity, dto.userId());
			entity = residentRepository.save(entity);
			return new ResidentDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	private void linkAssociations(Long dto, Resident entity, Long dto1) {
		Unit unit = unitRepository.getReferenceById(dto);
		entity.setUnit(unit);
		User user = userRepository.getReferenceById(dto1);
		entity.setUser(user);
	}
}
