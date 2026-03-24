package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.*;
import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.entities.*;
import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.mapper.PackageMapper;
import com.cefet_rj.projeto_final.repositories.PackageRepository;
import com.cefet_rj.projeto_final.repositories.UnitRepository;
import com.cefet_rj.projeto_final.repositories.UserRepository;
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
public class PackageService {

    private final PackageRepository packageRepository;

	private final PackageMapper packageMapper;

	private final UserRepository userRepository;

	private final UnitRepository unitRepository;


	@Transactional(readOnly = true)
    public Page<PackageDTO> findAllPaged(Pageable pageable) {
        Page<Package> list = packageRepository.findAll(pageable);
        return list.map(PackageDTO::new);
    }

    @Transactional(readOnly = true)
    public PackageDTO findById(Long id) {
        Optional<Package> obj = packageRepository.findById(id);
        Package entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return new PackageDTO(entity);
    }

	@Transactional
	public PackageDTO insert(PackageInsertDTO dto) {
		Package entity = new Package();
		packageMapper.copyInsertDtoToEntity(dto, entity);
		User recipient = userRepository.getReferenceById(dto.recipientId());
		entity.setRecipient(recipient);
		Unit unit = unitRepository.getReferenceById(dto.unitId());
		entity.setUnit(unit);
		entity = packageRepository.save(entity);
		return new PackageDTO(entity);
	}

	@Transactional
	public PackageDTO update(Long id, PackageUpdateDTO dto){
		try {
			Package entity = packageRepository.getReferenceById(id);
			packageMapper.copyUpdateDtoToEntity(dto, entity);
			entity = packageRepository.save(entity);
			return new PackageDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}
}
