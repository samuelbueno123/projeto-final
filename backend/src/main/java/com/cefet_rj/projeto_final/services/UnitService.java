package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.UnitDTO;
import com.cefet_rj.projeto_final.dtos.UnitInsertDTO;
import com.cefet_rj.projeto_final.dtos.UnitUpdateDTO;
import com.cefet_rj.projeto_final.entities.Sector;
import com.cefet_rj.projeto_final.entities.Unit;
import com.cefet_rj.projeto_final.mapper.UnitMapper;
import com.cefet_rj.projeto_final.repositories.SectorRepository;
import com.cefet_rj.projeto_final.repositories.UnitRepository;
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
public class UnitService {

	private final UnitRepository unitRepository;
	
	private final UnitMapper unitMapper;

	private final SectorRepository sectorRepository;
	
	@Transactional (readOnly = true)
	public Page <UnitDTO> findAllPaged(Pageable pageable) {
		Page <Unit> list = unitRepository.findAll(pageable);
		return list.map(UnitDTO::new);
	}

	@Transactional (readOnly = true)
	public UnitDTO findById(Long id) {
		Optional <Unit> obj = unitRepository.findById(id);
		Unit entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new UnitDTO(entity);
	}

	@Transactional
	public UnitDTO insert(UnitInsertDTO dto) {
		Unit entity = new Unit();
		unitMapper.copyInsertDtoToEntity(dto, entity);
		Sector sector = sectorRepository.getReferenceById(dto.sectorId());
		entity.setSector(sector);
		entity = unitRepository.save(entity);
		return new UnitDTO(entity);
	}

	@Transactional
	public UnitDTO update(Long id, UnitUpdateDTO dto){
		try {
			Unit entity = unitRepository.getReferenceById(id);
			unitMapper.copyUpdateDtoToEntity(dto, entity);
			entity = unitRepository.save(entity);
			return new UnitDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}
}
