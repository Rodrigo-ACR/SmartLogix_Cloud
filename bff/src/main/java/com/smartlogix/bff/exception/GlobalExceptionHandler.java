package com.smartlogix.bff.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================================
    // 🔥 CUALQUIER ERROR NO CONTROLADO
    // =========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarError(Exception e) {

        ApiError error = new ApiError(
                "Servicio temporalmente no disponible",
                503
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }
}