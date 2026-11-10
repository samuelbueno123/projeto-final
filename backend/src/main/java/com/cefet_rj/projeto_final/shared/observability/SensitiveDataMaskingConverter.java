package com.cefet_rj.projeto_final.shared.observability;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveDataMaskingConverter extends MessageConverter {

    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)\"?\\b(password|passwd|senha|client[_-]?secret|secret|secret[_-]?key|token|access[_-]?token|refresh[_-]?token|id[_-]?token)\"?\\s*[:=]\\s*\"?([^\"'\\s,;}{]+)\"?"
    );

    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "Bearer\\s+[A-Za-z0-9_\\-\\.~+/=]+"
    );

    private static final Pattern CREDIT_CARD = Pattern.compile(
            "\\b(?:\\d{4}[ -]?){3}\\d{4}\\b"
    );

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Mask sensitive key-values
        Matcher matcher = SENSITIVE_KEY_VALUE.matcher(message);
        if (matcher.find()) {
            StringBuffer sb = new StringBuffer();
            do {
                String key = matcher.group(1);
                matcher.appendReplacement(sb, key + "=\"***MASKED***\"");
            } while (matcher.find());
            matcher.appendTail(sb);
            message = sb.toString();
        }

        // Mask Bearer tokens
        message = BEARER_TOKEN.matcher(message).replaceAll("Bearer ***MASKED_JWT***");

        // Mask credit cards
        message = CREDIT_CARD.matcher(message).replaceAll("****-****-****-****");

        return message;
    }
}
