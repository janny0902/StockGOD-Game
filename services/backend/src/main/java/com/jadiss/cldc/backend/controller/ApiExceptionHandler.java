package com.jadiss.cldc.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Business logic: Convert invalid timeframe and similar argument errors into client-friendly 400 responses.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of(
                "error", "BAD_REQUEST",
                "message", exception.getMessage()
        );
    }

    // Business logic: Return conflict details when game session state does not allow requested operation.
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(IllegalStateException.class)
    public Map<String, Object> handleIllegalState(IllegalStateException exception) {
        return Map.of(
                "error", "CONFLICT",
                "message", exception.getMessage()
        );
    }

    // Business logic: Return validation error details when required fields are missing or malformed.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidationError(MethodArgumentNotValidException exception) {
        return Map.of(
                "error", "VALIDATION_FAILED",
                "message", exception.getMessage()
        );
    }
}
