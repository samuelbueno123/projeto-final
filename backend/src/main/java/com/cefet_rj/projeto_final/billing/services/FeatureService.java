package com.cefet_rj.projeto_final.billing.services;

import com.cefet_rj.projeto_final.billing.dtos.FeatureDTO;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;

    @Transactional(readOnly = true)
    public List<FeatureDTO> findAll() {

        return featureRepository
                .findAll()
                .stream()
                .map(FeatureDTO::new)
                .toList();
    }

    @Transactional
    public FeatureDTO upsert(FeatureDTO dto) {
        if (dto.code() == null || dto.code().isBlank() || dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o código e o nome do recurso.");
        }
        String normalizedCode = dto.code().trim().toUpperCase(Locale.ROOT);
        Feature feature = featureRepository.findByCode(normalizedCode)
                .orElseGet(() -> {
                    Feature created = new Feature();
                    created.setCode(normalizedCode);
                    return created;
                });
        feature.setName(dto.name().trim());
        feature.setDescription(dto.description() == null || dto.description().isBlank() ? null : dto.description().trim());
        feature = featureRepository.save(feature);
        return new FeatureDTO(feature);
    }
}
