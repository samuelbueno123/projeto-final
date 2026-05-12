package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.dtos.insert.PackageInsertDTO;
import com.cefet_rj.projeto_final.dtos.update.PackageUpdateDTO;
import com.cefet_rj.projeto_final.entities.*;
import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.repositories.PackageRepository;
import com.cefet_rj.projeto_final.repositories.UnitRepository;
import com.cefet_rj.projeto_final.repositories.UserRepository;
import com.cefet_rj.projeto_final.services.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
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
public class PackageService {

	private final PackageRepository packageRepository;

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
		linkAssociations(dto.description(), dto.status(), dto.arrivalAt(), dto.pickedUpAt(), dto.recipientId(), dto.unitId(), entity);
		entity = packageRepository.save(entity);
		return new PackageDTO(entity);
	}

	@Transactional
	public PackageDTO update(Long id, PackageUpdateDTO dto){
		try {
			Package entity = packageRepository.getReferenceById(id);
			linkAssociations(dto.description(), dto.status(), dto.arrivalAt(), dto.pickedUpAt(), dto.recipientId(), dto.unitId(), entity);
			entity = packageRepository.save(entity);
			return new PackageDTO(entity);
		}
		catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException("Id " + id + " not found");
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void delete(Long id) {
		if (!packageRepository.existsById(id)) {
			throw new ResourceNotFoundException("Resource not found");
		}
		try {
			packageRepository.deleteById(id);
		}
		catch (DataIntegrityViolationException e) {
			throw new DatabaseException("Integrity violation");
		}
	}

	private void linkAssociations(String description, com.cefet_rj.projeto_final.entities.enums.PackageStatus status,
								  java.time.LocalDateTime arrivalAt, java.time.LocalDateTime pickedUpAt,
								  Long recipientId, Long unitId, Package entity) {
		entity.setDescription(description);
		entity.setStatus(status);
		entity.setArrivalAt(arrivalAt);
		entity.setPickedUpAt(pickedUpAt);
		User recipient = userRepository.getReferenceById(recipientId);
		entity.setRecipient(recipient);
		Unit unit = unitRepository.getReferenceById(unitId);
		entity.setUnit(unit);
	}
}
