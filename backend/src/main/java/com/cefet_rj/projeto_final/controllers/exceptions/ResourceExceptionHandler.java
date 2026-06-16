package com.cefet_rj.projeto_final.controllers.exceptions;

import com.cefet_rj.projeto_final.services.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.services.exceptions.InvalidStatusException;
import com.cefet_rj.projeto_final.services.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.List;

@ControllerAdvice
public class ResourceExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> entityNotFound(ResourceNotFoundException e, HttpServletRequest request){
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Resource not found",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<StandardError> database(DatabaseException e, HttpServletRequest request){
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Database exception",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> validation(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<FieldMessage> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> new FieldMessage(
                        f.getField(),
                        f.getDefaultMessage()
                ))
                .toList();

        ValidationError err = new ValidationError(
                Instant.now(),
                status.value(),
                "Validation exception",
                "Validation failed",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<StandardError> invalidStatus(
            InvalidStatusException e,
            HttpServletRequest request){

        HttpStatus status = HttpStatus.BAD_REQUEST;

        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Invalid package status",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(err);
    }
}