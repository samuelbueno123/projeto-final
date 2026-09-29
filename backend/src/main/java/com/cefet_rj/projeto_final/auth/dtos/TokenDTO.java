package com.cefet_rj.projeto_final.auth.dtos;

public record TokenDTO(
        String accessToken,
        String tokenType,
        Long expiresIn
) {}
