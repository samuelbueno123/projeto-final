package com.cefet_rj.projeto_final.shared.observability;

public final class ObservabilityConstants {

    private ObservabilityConstants() {}

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_MDC_KEY = "currentUser";
}
