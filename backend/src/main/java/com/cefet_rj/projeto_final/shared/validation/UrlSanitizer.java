package com.cefet_rj.projeto_final.shared.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Locale;

/**
 * Sanitizador centralizado para URLs de mídia e anexos fornecidos por usuários,
 * bloqueando esquemas inseguros (javascript:, vbscript:, data:text/html, SVG inline XSS e path traversal).
 */
public final class UrlSanitizer {

    private UrlSanitizer() {
    }

    public static String sanitizeMediaUrl(String rawUrl, String fieldName) {
        return sanitize(rawUrl, fieldName, true);
    }

    public static String sanitizeHttpOrRelativeUrl(String rawUrl, String fieldName) {
        return sanitize(rawUrl, fieldName, false);
    }

    private static String sanitize(String rawUrl, String fieldName, boolean allowSafeImageDataUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:")
                || lower.startsWith("vbscript:")
                || lower.startsWith("file:")
                || lower.startsWith("data:text/")
                || lower.startsWith("data:application/")
                || lower.startsWith("data:image/svg")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " contém um esquema de URL não permitido."
            );
        }

        if (trimmed.startsWith("/media/") || trimmed.startsWith("media/")) {
            if (trimmed.contains("..") || trimmed.contains("\\")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        fieldName + " contém caminho relativo inválido."
                );
            }
            return trimmed;
        }

        if (lower.startsWith("data:")) {
            if (allowSafeImageDataUrl && lower.matches("^data:image/(png|jpeg|jpg|webp);base64,[a-z0-9+/=\\r\\n]+$")) {
                return trimmed;
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " deve ser uma URL HTTP/HTTPS válida ou imagem PNG/JPEG/WebP."
            );
        }

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        fieldName + " deve ser uma URL HTTP ou HTTPS válida."
                );
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " não possui formato de URL válido."
            );
        }
    }
}
