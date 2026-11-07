package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.condominiums.dtos.UnitCsvImportResultDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.UnitDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.UnitInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.UnitUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository unitRepository;
    private final SectorRepository sectorRepository;
    private final CondominiumRepository condominiumRepository;
    private final CondominiumAccessService condominiumAccessService;

    @Transactional(readOnly = true)
    public Page<UnitDTO> findAllPaged(Pageable pageable) {
        Page<Unit> list = unitRepository.findAll(pageable);
        return list.map(UnitDTO::new);
    }

    @Transactional(readOnly = true)
    public UnitDTO findById(Long id) {
        Optional<Unit> obj = unitRepository.findById(id);
        Unit entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        condominiumAccessService.validateAccess(entity.getSector().getCondominium().getId());
        return new UnitDTO(entity);
    }

    @Transactional
    public UnitDTO insert(UnitInsertDTO dto) {
        Unit entity = new Unit();
        linkAssociations(dto.identifier(), dto.sectorId(), entity);
        condominiumAccessService.validateManagerAccess(entity.getSector().getCondominium().getId());
        entity = unitRepository.save(entity);
        return new UnitDTO(entity);
    }

    @Transactional
    public UnitDTO update(Long id, UnitUpdateDTO dto) {
        try {
            Unit entity = unitRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Id " + id + " not found"));
            condominiumAccessService.validateManagerAccess(entity.getSector().getCondominium().getId());
            linkAssociations(dto.identifier(), dto.sectorId(), entity);
            condominiumAccessService.validateManagerAccess(entity.getSector().getCondominium().getId());
            entity = unitRepository.save(entity);
            return new UnitDTO(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Id " + id + " not found");
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {
        Unit entity = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        condominiumAccessService.validateManagerAccess(entity.getSector().getCondominium().getId());
        try {
            unitRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }
    }

    @Transactional
    public UnitCsvImportResultDTO importCsv(Long condominiumId, MultipartFile file) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        int totalProcessed = 0;
        int importedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            int sectorIdx = 0;
            int identifierIdx = 1;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split("[,;]");
                if (parts.length < 2) {
                    if (parts.length == 1 && !parts[0].trim().isEmpty()) {
                        parts = new String[] { "Geral", parts[0].trim() };
                    } else {
                        continue;
                    }
                }

                if (isFirstLine) {
                    isFirstLine = false;
                    String col0 = parts[0].trim().toLowerCase();
                    String col1 = parts[1].trim().toLowerCase();
                    if (col0.contains("setor") || col0.contains("sector") || col0.contains("bloco")) {
                        sectorIdx = 0;
                        identifierIdx = 1;
                        continue;
                    } else if (col1.contains("setor") || col1.contains("sector") || col1.contains("bloco")) {
                        sectorIdx = 1;
                        identifierIdx = 0;
                        continue;
                    } else if (col0.contains("unidade") || col0.contains("unit") || col0.contains("identificador")) {
                        identifierIdx = 0;
                        sectorIdx = 1;
                        continue;
                    }
                }

                totalProcessed++;
                String sectorName = parts[sectorIdx].trim();
                String unitIdentifier = parts[identifierIdx].trim();

                if (sectorName.isEmpty()) sectorName = "Geral";
                if (unitIdentifier.isEmpty()) {
                    skippedCount++;
                    errors.add("Linha ignorada: identificador da unidade em branco.");
                    continue;
                }

                final String finalSectorName = sectorName;
                Sector sector = sectorRepository.findByCondominiumIdAndNameIgnoreCase(condominiumId, finalSectorName)
                        .orElseGet(() -> {
                            Sector newSector = new Sector();
                            newSector.setName(finalSectorName);
                            newSector.setStructuralType(SectorStructuralType.BLOCK);
                            newSector.setCondominium(condominium);
                            return sectorRepository.save(newSector);
                        });

                if (unitRepository.existsBySectorIdAndIdentifierIgnoreCase(sector.getId(), unitIdentifier)) {
                    skippedCount++;
                    errors.add("Unidade '" + unitIdentifier + "' já cadastrada no setor '" + sectorName + "'.");
                    continue;
                }

                Unit unit = new Unit();
                unit.setIdentifier(unitIdentifier);
                unit.setSector(sector);
                unitRepository.save(unit);
                importedCount++;
            }
        } catch (Exception e) {
            throw new DatabaseException("Erro ao processar arquivo CSV: " + e.getMessage());
        }

        return new UnitCsvImportResultDTO(totalProcessed, importedCount, skippedCount, errors);
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
