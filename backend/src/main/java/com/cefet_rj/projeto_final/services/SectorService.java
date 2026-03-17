package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.entities.Sector;
import com.cefet_rj.projeto_final.entities.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.repositories.SectorRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SectorService {

	private final SectorRepository sectorRepository;

	private final CondominiumRepository condominiumRepository;

	private final CondominiumService condominiumService;

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
	public SectorDTO insert(SectorDTO dto) {
		Sector entity = new Sector();

		entity.setName(dto.name());
		entity.setStructuralType(SectorStructuralType.valueOf(dto.structuralType()));
		entity.setCondominium(condominiumRepository.getReferenceById(dto.condominiumId()));

		entity = sectorRepository.save(entity);
		return new SectorDTO(entity);
	}

	public void copyDtoToEntity(SectorDTO dto, Sector entity) {
		Condominium condominiumEntity = new Condominium();

		CondominiumDTO condominium = condominiumService.findById(dto.condominiumId());

		condominiumService.copyDtoToEntity(condominium, condominiumEntity);

		entity.setName(dto.name());
		entity.setStructuralType(SectorStructuralType.valueOf(dto.structuralType()));
		entity.setCondominium(condominiumEntity);
	}

}
