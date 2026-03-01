package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.services.CondominiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/condominiums")
@RequiredArgsConstructor
public class CondominiumController {

    private final CondominiumService condominiumService;
}
