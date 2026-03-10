package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.*;
import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.entities.*;
import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.entities.enums.PackageStatus;
import com.cefet_rj.projeto_final.repositories.PackageRepository;
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
	private final UserService userService;
	private final UnitService unitService;

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
	public PackageDTO update(Long id, PackageDTO dto){
		try {
			Package entity = packageRepository.getReferenceById(id);
			copyDtoToEntity(dto, entity);
			entity = packageRepository.save(entity);
			return new PackageDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}
	
	public void copyDtoToEntity(PackageDTO dto, Package entity) {
		User userEntity = new User();
		Unit unitEntity = new Unit();

		UserDTO recipient = userService.findById(dto.recipientId());
		UnitDTO unit = unitService.findById(dto.unitId());

		userService.copyDtoToEntity(recipient, userEntity);
		unitService.copyDtoToEntity(unit, unitEntity);

		entity.setDescription(dto.description());
		entity.setRecipient(userEntity);
		entity.setUnit(unitEntity);
		entity.setStatus(PackageStatus.valueOf(dto.status()));
		entity.setArrivalAt(dto.arrivalAt());
		entity.setPickedUpAt(dto.pickedUpAt());
	}
}
