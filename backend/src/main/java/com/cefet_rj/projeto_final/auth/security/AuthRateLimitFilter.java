package com.cefet_rj.projeto_final.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro leve de rate limiting em janela fixa de 1 minuto (via Caffeine) para endpoints
 * críticos de autenticação, cadastro, retirada por código e validação de QR Code de visitantes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, AtomicInteger> buckets = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(25_000)
            .build();

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled = true;

    @Value("${app.rate-limit.auth-per-minute:300}")
    private int authPerMinute = 300;

    @Value("${app.rate-limit.account-per-minute:30}")
    private int accountPerMinute = 30;

    @Value("${app.rate-limit.pin-per-minute:30}")
    private int pinPerMinute = 30;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = normalizePath(request);
        String ip = resolveClientIp(request);

        if ("/oauth2/token".equals(uri) || "/auth/google".equals(uri) || "/users".equals(uri)) {
            if (isOverLimit("AUTH_IP:" + uri + ":" + ip, authPerMinute)) {
                writeRateLimitResponse(response);
                return;
            }
            String username = request.getParameter("username");
            if ("/oauth2/token".equals(uri) && username != null && !username.isBlank()) {
                String accountKey = "AUTH_ACCT:" + ip + ":" + username.trim().toLowerCase(java.util.Locale.ROOT);
                if (isOverLimit(accountKey, accountPerMinute)) {
                    writeRateLimitResponse(response);
                    return;
                }
            }
        } else if (uri.matches("^/lockers/\\d+/retrieve$")
                || "/community/visitors/validate-qr".equals(uri)
                || uri.matches("^/condominiums/\\d+/visitors/validate-qr$")) {
            if (isOverLimit("PIN_VERIFY:" + ip, pinPerMinute)) {
                writeRateLimitResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isOverLimit(String key, int limit) {
        if (limit <= 0) return false;
        AtomicInteger counter = buckets.get(key, k -> new AtomicInteger(0));
        return counter != null && counter.incrementAndGet() > limit;
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"Muitas tentativas. Aguarde um minuto antes de tentar novamente.\",\"status\":429}");
    }

    private String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String addr = request.getRemoteAddr();
        return addr != null ? addr : "unknown";
    }
}
