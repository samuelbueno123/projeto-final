package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.condominiums.dtos.UnitDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.UnitInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.UnitUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
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
public class UnitService {

	private final UnitRepository unitRepository;
	
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
		linkAssociations(dto.identifier(), dto.sectorId(), entity);
		entity = unitRepository.save(entity);
		return new UnitDTO(entity);
	}

	@Transactional
	public UnitDTO update(Long id, UnitUpdateDTO dto){
		try {
			Unit entity = unitRepository.getReferenceById(id);
			linkAssociations(dto.identifier(), dto.sectorId(), entity);
			entity = unitRepository.save(entity);
			return new UnitDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!unitRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			unitRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	private void linkAssociations(
			String identifier,
			Long sectorId,
			Unit entity
	) {
		entity.setIdentifier(identifier);

		Sector sector = sectorRepository.findById(sectorId)
				.orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + sectorId));

		entity.setSector(sector);
	}
}
