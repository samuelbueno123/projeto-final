package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.dtos.PackageDTO;
import com.cefet_rj.projeto_final.entities.Package;
import com.cefet_rj.projeto_final.repositories.PackageRepository;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
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
}
