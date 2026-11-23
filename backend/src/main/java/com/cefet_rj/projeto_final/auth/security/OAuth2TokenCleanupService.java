package com.cefet_rj.projeto_final.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;

/**
 * Gerencia revogação imediata de sessões OAuth2 (ex.: alteração de senha/e-mail)
 * e expurgo periódico de tokens expirados na tabela oauth2_authorization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenCleanupService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int revokeTokensForPrincipal(String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            return 0;
        }
        String normalized = principalEmail.trim().toLowerCase(Locale.ROOT);
        int deleted = jdbcTemplate.update(
                "DELETE FROM oauth2_authorization WHERE LOWER(principal_name) = ?",
                normalized
        );
        log.debug("Revoked {} OAuth2 authorization rows for principal={}", deleted, normalized);
        return deleted;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredAuthorizations() {
        Timestamp now = Timestamp.from(Instant.now());
        int purged = jdbcTemplate.update(
                """
                DELETE FROM oauth2_authorization
                WHERE (refresh_token_expires_at IS NULL OR refresh_token_expires_at < ?)
                  AND (access_token_expires_at IS NULL OR access_token_expires_at < ?)
                  AND (authorization_code_expires_at IS NULL OR authorization_code_expires_at < ?)
                """,
                now,
                now,
                now
        );
        if (purged > 0) {
            log.info("Purged {} expired OAuth2 authorization entries", purged);
        }
    }
}
