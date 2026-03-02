package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.entities.Condominium;
import com.cefet_rj.projeto_final.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CondominiumService {

    private final CondominiumRepository condominiumRepository;

    @Transactional(readOnly = true)
    public Page<CondominiumDTO> findAllPaged(Pageable pageable) {
        Page<Condominium> list = condominiumRepository.findAll(pageable);
        return list.map(CondominiumDTO::new);
    }

    @Transactional(readOnly = true)
    public CondominiumDTO findById(Long id) {
        Optional<Condominium> obj = condominiumRepository.findById(id);
        Condominium entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        return new CondominiumDTO(entity);
    }
}
