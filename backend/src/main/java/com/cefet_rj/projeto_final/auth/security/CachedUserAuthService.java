package com.cefet_rj.projeto_final.auth.security;

import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Serviço em cache para resolução de authorities do usuário em requisições JWT,
 * evitando consultas repetitivas ao banco a cada requisição autenticada.
 */
@Service
@RequiredArgsConstructor
public class CachedUserAuthService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "userAuth",
            key = "#email == null ? '' : #email.trim().toLowerCase(T(java.util.Locale).ROOT)",
            condition = "#email != null && !#email.isBlank()"
    )
    public List<String> getAuthorityNamesByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Collections.emptyList();
        }
        return userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .map(user -> user.getRoles().stream()
                        .map(role -> role.getAuthority())
                        .toList())
                .orElse(Collections.emptyList());
    }

    @CacheEvict(
            value = "userAuth",
            key = "#email == null ? '' : #email.trim().toLowerCase(T(java.util.Locale).ROOT)",
            condition = "#email != null && !#email.isBlank()"
    )
    public void evictUserAuth(String email) {
        // Invalidado após mudança de perfil, senha, email ou vínculo de papéis
    }
}
