package com.cefet_rj.projeto_final.billing.services;

import com.cefet_rj.projeto_final.billing.dtos.FeatureDTO;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}