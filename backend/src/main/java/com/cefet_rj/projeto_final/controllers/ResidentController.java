package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.services.ResidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;
}
