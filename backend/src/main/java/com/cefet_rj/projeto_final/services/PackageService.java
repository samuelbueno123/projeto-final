package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.repositories.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
}
