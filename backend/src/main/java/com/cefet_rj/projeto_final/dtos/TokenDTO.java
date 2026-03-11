package com.cefet_rj.projeto_final.dtos;

public record TokenDTO(
        String accessToken,
        String tokenType,
        Long expiresIn
) {}
