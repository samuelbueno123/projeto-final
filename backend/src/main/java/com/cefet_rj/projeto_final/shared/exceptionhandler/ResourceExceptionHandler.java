package com.cefet_rj.projeto_final.shared.exceptionhandler;

import com.cefet_rj.projeto_final.packages.exceptions.InvalidPackageStateTransitionException;
import com.cefet_rj.projeto_final.packages.exceptions.InvalidStatusException;
import com.cefet_rj.projeto_final.reservations.exceptions.ReservationConflictException;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> resourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.NOT_FOUND;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<StandardError> forbidden(
            ForbiddenException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.FORBIDDEN;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<StandardError> reservationConflict(
            ReservationConflictException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.CONFLICT;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(InvalidPackageStateTransitionException.class)
    public ResponseEntity<StandardError> invalidPackageTransition(
            InvalidPackageStateTransitionException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.CONFLICT;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<StandardError> invalidStatus(
            InvalidStatusException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.BAD_REQUEST;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardError> database(
            DatabaseException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.BAD_REQUEST;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> dataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.CONFLICT;

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        "Database integrity violation",
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        HttpStatus status =
                HttpStatus.BAD_REQUEST;

        ValidationError error =
                new ValidationError(
                        Instant.now(),
                        status.value(),
                        "Validation error",
                        "One or more fields are invalid",
                        request.getRequestURI()
                );

        for (FieldError fieldError :
                exception
                        .getBindingResult()
                        .getFieldErrors()) {

            error.addError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return ResponseEntity
                .status(status)
                .body(error);
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

        StandardError error =
                new StandardError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getReason() != null ? exception.getReason() : exception.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardError> missingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Parâmetro obrigatório ausente: " + exception.getParameterName(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
