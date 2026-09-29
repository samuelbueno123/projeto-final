package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.condominiums.dtos.SectorDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.SectorUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
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
public class SectorService {

	private final SectorRepository sectorRepository;

	private final CondominiumRepository condominiumRepository;
	
	@Transactional(readOnly = true)
	public Page<SectorDTO> findAllPaged(Pageable pageable) {
		Page<Sector> list = sectorRepository.findAll(pageable);
		return list.map(SectorDTO::new);
	}

	@Transactional(readOnly = true)
	public SectorDTO findById(Long id) {
		Optional<Sector> obj = sectorRepository.findById(id);
		Sector entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new SectorDTO(entity);
	}

	@Transactional
	public SectorDTO insert(SectorInsertDTO dto) {
		Sector entity = new Sector();
		linkAssociations(dto.name(), dto.structuralType(), dto.condominiumId(), entity);
		entity = sectorRepository.save(entity);
		return new SectorDTO(entity);
	}

	@Transactional
	public SectorDTO update(Long id, SectorUpdateDTO dto){
		try {
			Sector entity = sectorRepository.getReferenceById(id);
			linkAssociations(dto.name(), dto.structuralType(), dto.condominiumId(), entity);
			entity = sectorRepository.save(entity);
			return new SectorDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!sectorRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			sectorRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	private void linkAssociations(
			String name,
			String structuralType,
			Long condominiumId,
			Sector entity
	) {
		entity.setName(name);
		entity.setStructuralType(SectorStructuralType.fromString(structuralType));
		Condominium condominium = condominiumRepository.findById(condominiumId)
				.orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));
		entity.setCondominium(condominium);
	}
}
