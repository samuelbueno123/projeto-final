package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.dtos.UnitDTO;
import com.cefet_rj.projeto_final.dtos.UserDTO;
import com.cefet_rj.projeto_final.entities.Resident;
import com.cefet_rj.projeto_final.entities.Unit;
import com.cefet_rj.projeto_final.entities.User;
import com.cefet_rj.projeto_final.entities.enums.ResidentType;
import com.cefet_rj.projeto_final.repositories.ResidentRepository;
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
public class ResidentService {

    private final ResidentRepository residentRepository;
	private final UserService userService;
	private final UnitService unitService;

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
	public ResidentDTO update(Long id, ResidentDTO dto){
		try {
			Resident entity = residentRepository.getReferenceById(id);
			copyDtoToEntity(dto, entity);
			entity = residentRepository.save(entity);
			return new ResidentDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	public void copyDtoToEntity(ResidentDTO dto, Resident entity) {
		User userEntity = new User();
		Unit unitEntity = new Unit();

		UserDTO user = userService.findById(dto.userId());
		UnitDTO unit = unitService.findById(dto.unitId());

		userService.copyDtoToEntity(user, userEntity);
		unitService.copyDtoToEntity(unit, unitEntity);

		entity.setUser(userEntity);
		entity.setUnit(unitEntity);
		entity.setType(ResidentType.valueOf(dto.type()));
	}
}
