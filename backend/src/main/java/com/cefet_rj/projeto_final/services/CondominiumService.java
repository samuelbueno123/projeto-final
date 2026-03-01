package com.cefet_rj.projeto_final.services;

import com.cefet_rj.projeto_final.repositories.CondominiumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CondominiumService {

    private final CondominiumRepository condominiumRepository;
}
