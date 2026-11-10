package com.cefet_rj.projeto_final.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        MDC.put(ObservabilityConstants.CORRELATION_ID_MDC_KEY, correlationId);
        request.setAttribute(ObservabilityConstants.CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(ObservabilityConstants.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
            MDC.remove(ObservabilityConstants.USER_MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(ObservabilityConstants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(ObservabilityConstants.REQUEST_ID_HEADER);
        }

        if (correlationId != null) {
            correlationId = correlationId.trim();
            if (SAFE_CORRELATION_ID.matcher(correlationId).matches()) {
                return correlationId;
            }
        }

        return UUID.randomUUID().toString();
    }
}
