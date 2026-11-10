package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.condominiums.dtos.ResidentDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.ResidentInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.ResidentUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final CondominiumAccessService condominiumAccessService;

    @Transactional(readOnly = true)
    public Page<ResidentDTO> findAllPaged(Pageable pageable) {
        Page<Resident> list = residentRepository.findAll(pageable);
        return list.map(ResidentDTO::new);
    }

    @Transactional(readOnly = true)
    public ResidentDTO findById(Long id) {
        Optional<Resident> obj = residentRepository.findById(id);
        Resident entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        condominiumAccessService.validateManagerAccess(entity.getUnit().getSector().getCondominium().getId());
        return new ResidentDTO(entity);
    }

    @Transactional
    public ResidentDTO insert(ResidentInsertDTO dto) {
        if (residentRepository.existsByUserIdAndUnitId(dto.userId(), dto.unitId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já está cadastrado como morador desta unidade.");
        }
        Resident entity = new Resident();
        linkAssociations(dto.type(), dto.unitId(), dto.userId(), entity);
        condominiumAccessService.validateManagerAccess(entity.getUnit().getSector().getCondominium().getId());
        entity = residentRepository.save(entity);
        return new ResidentDTO(entity);
    }

    @Transactional
    public ResidentDTO update(Long id, ResidentUpdateDTO dto) {
        try {
            Resident entity = residentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Id " + id + " not found"));
            condominiumAccessService.validateManagerAccess(entity.getUnit().getSector().getCondominium().getId());
            residentRepository.findByUserIdAndUnitId(dto.userId(), dto.unitId())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já está vinculado a esta unidade em outro registro.");
                    });
            linkAssociations(dto.type(), dto.unitId(), dto.userId(), entity);
            condominiumAccessService.validateManagerAccess(entity.getUnit().getSector().getCondominium().getId());
            entity = residentRepository.save(entity);
            return new ResidentDTO(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Id " + id + " not found");
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {
        Resident entity = residentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        condominiumAccessService.validateManagerAccess(entity.getUnit().getSector().getCondominium().getId());
        try {
            residentRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }
    }

    private void linkAssociations(
            String type,
            Long unitId,
            Long userId,
            Resident entity
    ) {
        entity.setType(ResidentType.fromString(type));

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + unitId));
        entity.setUnit(unit);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        entity.setUser(user);
    }
}
