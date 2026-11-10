package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.condominiums.dtos.SectorDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.SectorInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.SectorUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final CondominiumRepository condominiumRepository;
    private final CondominiumAccessService condominiumAccessService;

    @Transactional(readOnly = true)
    public Page<SectorDTO> findAllPaged(Pageable pageable) {
        Page<Sector> list = sectorRepository.findAll(pageable);
        return list.map(SectorDTO::new);
    }

    @Transactional(readOnly = true)
    public List<SectorDTO> findByCondominiumId(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        return sectorRepository.findByCondominiumId(condominiumId).stream()
                .map(SectorDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectorDTO findById(Long id) {
        Optional<Sector> obj = sectorRepository.findById(id);
        Sector entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        condominiumAccessService.validateAccess(entity.getCondominium().getId());
        return new SectorDTO(entity);
    }

    @Transactional
    public SectorDTO insert(SectorInsertDTO dto) {
        condominiumAccessService.validateManagerAccess(dto.condominiumId());
        if (dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do setor é obrigatório.");
        }
        if (sectorRepository.findByCondominiumIdAndNameIgnoreCase(dto.condominiumId(), dto.name().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um setor com este nome neste condomínio.");
        }
        Sector entity = new Sector();
        linkAssociations(dto.name().trim(), dto.structuralType(), dto.condominiumId(), dto.imageUrl(), entity);
        entity = sectorRepository.save(entity);
        return new SectorDTO(entity);
    }

    @Transactional
    public SectorDTO update(Long id, SectorUpdateDTO dto) {
        try {
            Sector entity = sectorRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Id " + id + " not found"));
            condominiumAccessService.validateManagerAccess(entity.getCondominium().getId());
            condominiumAccessService.validateManagerAccess(dto.condominiumId());
            if (dto.name() != null && !dto.name().isBlank()) {
                String newName = dto.name().trim();
                sectorRepository.findByCondominiumIdAndNameIgnoreCase(dto.condominiumId(), newName)
                        .filter(other -> !other.getId().equals(id))
                        .ifPresent(other -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe outro setor com este nome neste condomínio.");
                        });
            }
            linkAssociations(dto.name(), dto.structuralType(), dto.condominiumId(), dto.imageUrl(), entity);
            entity = sectorRepository.save(entity);
            return new SectorDTO(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Id " + id + " not found");
        }
    }

    @Transactional
    public SectorDTO updateImageUrl(Long id, String imageUrl) {
        Sector entity = sectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + id));
        condominiumAccessService.validateManagerAccess(entity.getCondominium().getId());
        entity.setImageUrl(imageUrl);
        entity = sectorRepository.save(entity);
        return new SectorDTO(entity);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {
        Sector entity = sectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        condominiumAccessService.validateManagerAccess(entity.getCondominium().getId());
        try {
            sectorRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }
    }

    private void linkAssociations(
            String name,
            String structuralType,
            Long condominiumId,
            String imageUrl,
            Sector entity
    ) {
        entity.setName(name);
        entity.setStructuralType(SectorStructuralType.fromString(structuralType));
        if (imageUrl != null) {
            entity.setImageUrl(imageUrl);
        }
        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));
        entity.setCondominium(condominium);
    }
}
