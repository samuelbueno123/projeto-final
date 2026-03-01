package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.repositories.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
}
