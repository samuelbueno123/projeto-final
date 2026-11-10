package com.cefet_rj.projeto_final.shared.exceptionhandler;

import com.cefet_rj.projeto_final.packages.exceptions.InvalidPackageStateTransitionException;
import com.cefet_rj.projeto_final.packages.exceptions.InvalidStatusException;
import com.cefet_rj.projeto_final.reservations.exceptions.ReservationConflictException;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.shared.observability.ObservabilityConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@ControllerAdvice
public class ResourceExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResourceExceptionHandler.class);

    private String getCorrelationId(HttpServletRequest request) {
        Object attr = request.getAttribute(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        String mdc = MDC.get(ObservabilityConstants.CORRELATION_ID_MDC_KEY);
        if (mdc != null && !mdc.isBlank()) {
            return mdc;
        }
        String header = request.getHeader(ObservabilityConstants.CORRELATION_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> resourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String correlationId = getCorrelationId(request);

        log.warn("Resource not found [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardError> forbidden(
            ForbiddenException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String correlationId = getCorrelationId(request);

        log.warn("Forbidden access [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> accessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        String correlationId = getCorrelationId(request);

        log.warn("Access denied [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Acesso negado: permissões insuficientes para este recurso",
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StandardError> authentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String correlationId = getCorrelationId(request);

        log.warn("Authentication failed [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Não autenticado: credenciais ausentes ou inválidas",
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<StandardError> reservationConflict(
            ReservationConflictException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        String correlationId = getCorrelationId(request);

        log.warn("Reservation conflict [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(InvalidPackageStateTransitionException.class)
    public ResponseEntity<StandardError> invalidPackageTransition(
            InvalidPackageStateTransitionException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        String correlationId = getCorrelationId(request);

        log.warn("Invalid package transition [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<StandardError> invalidStatus(
            InvalidStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Invalid status [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardError> database(
            DatabaseException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Database exception [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> dataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        String correlationId = getCorrelationId(request);

        log.warn("Data integrity violation [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Violação de integridade referencial ou chave duplicada",
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Validation error [correlationId={}, path={}]: {} field errors",
                correlationId, request.getRequestURI(), exception.getBindingResult().getFieldErrorCount());

        ValidationError error = new ValidationError(
                Instant.now(),
                status.value(),
                "Validation error",
                "Um ou mais campos contêm valores inválidos",
                request.getRequestURI(),
                correlationId
        );

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            error.addError(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardError> missingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Missing parameter [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getParameterName());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Parâmetro obrigatório ausente: " + exception.getParameterName(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> messageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Unreadable HTTP message [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Corpo da requisição ausente ou formato JSON inválido",
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardError> illegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Illegal argument [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<StandardError> illegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String correlationId = getCorrelationId(request);

        log.warn("Illegal state [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<StandardError> methodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        String correlationId = getCorrelationId(request);

        log.warn("Method not allowed [correlationId={}, path={}, method={}]: {}",
                correlationId, request.getRequestURI(), exception.getMethod(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Método HTTP '" + exception.getMethod() + "' não suportado para este endpoint",
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<StandardError> mediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String correlationId = getCorrelationId(request);

        log.warn("Media type not supported [correlationId={}, path={}, contentType={}]: {}",
                correlationId, request.getRequestURI(), exception.getContentType(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Tipo de mídia não suportado: " + exception.getContentType(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<StandardError> responseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String correlationId = getCorrelationId(request);

        log.warn("Response status exception [correlationId={}, path={}, status={}]: {}",
                correlationId, request.getRequestURI(), status.value(), exception.getReason());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                exception.getReason() != null ? exception.getReason() : exception.getMessage(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<StandardError> optimisticLockConflict(
            Exception exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        String correlationId = getCorrelationId(request);

        log.warn("Optimistic locking conflict [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage());

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Conflito de concorrência: o registro foi atualizado simultaneamente por outro usuário. Recarregue os dados e tente novamente.",
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> genericException(
            Exception exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String correlationId = getCorrelationId(request);

        log.error("Unhandled internal server error [correlationId={}, path={}]: {}",
                correlationId, request.getRequestURI(), exception.getMessage(), exception);

        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Ocorreu um erro interno inesperado no servidor. Código de rastreamento: " + correlationId,
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.status(status).body(error);
    }
}
