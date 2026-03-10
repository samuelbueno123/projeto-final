package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.UnitDTO;
import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.entities.Unit;
import com.cefet_rj.projeto_final.entities.Sector;
import com.cefet_rj.projeto_final.repositories.UnitRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnitService {

	private final UnitRepository UnitRepository;

	private final SectorService sectorService;

	@Transactional (readOnly = true)
	public Page <UnitDTO> findAllPaged(Pageable pageable) {
		Page <Unit> list = UnitRepository.findAll(pageable);
		return list.map(UnitDTO::new);
	}

	@Transactional (readOnly = true)
	public UnitDTO findById(Long id) {
		Optional <Unit> obj = UnitRepository.findById(id);
		Unit entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
		return new UnitDTO(entity);
	}

	public void copyDtoToEntity(UnitDTO dto, Unit entity) {
		Sector sectorEntity = new Sector();

		SectorDTO sector = sectorService.findById(dto.sectorId());

		//Need to create method
		sectorService.copyDtoToEntity(sector, sectorEntity);

		entity.setIdentifier(dto.identifier());
		entity.setSector(sectorEntity);
	}
}
