package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.SectorDTO;
import com.cefet_rj.projeto_final.dtos.SectorInsertDTO;
import com.cefet_rj.projeto_final.dtos.SectorUpdateDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.entities.Sector;
import com.cefet_rj.projeto_final.mapper.SectorMapper;
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
public class SectorService {

	private final SectorRepository sectorRepository;

	private final CondominiumRepository condominiumRepository;
	
	private final SectorMapper sectorMapper;

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
		sectorMapper.copyInsertDtoToEntity(dto, entity);
		Condominium condominium = condominiumRepository.getReferenceById(dto.condominiumId());
		entity.setCondominium(condominium);
		entity = sectorRepository.save(entity);
		return new SectorDTO(entity);
	}

	@Transactional
	public SectorDTO update(Long id, SectorUpdateDTO dto){
		try {
			Sector entity = sectorRepository.getReferenceById(id);
			sectorMapper.copyUpdateDtoToEntity(dto, entity);
			entity = sectorRepository.save(entity);
			return new SectorDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}
}
