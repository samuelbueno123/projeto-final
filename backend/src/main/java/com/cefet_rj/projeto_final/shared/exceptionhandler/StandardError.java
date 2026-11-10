package com.cefet_rj.projeto_final.shared.exceptionhandler;

import com.cefet_rj.projeto_final.shared.observability.ObservabilityConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.MDC;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StandardError {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;
    private String correlationId;

    public StandardError(
            Instant timestamp,
            Integer status,
            String error,
            String message,
            String path
    ) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.correlationId = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
    }
}