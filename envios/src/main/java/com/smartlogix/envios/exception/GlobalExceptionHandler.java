package com.smartlogix.envios.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException e) {

        String msg = e.getMessage();

        if (msg != null && msg.contains("no encontrado")) {
            return ResponseEntity.status(404).body(new ApiError(msg, 404));
        }

        if (msg != null && msg.contains("Transición de estado no permitida")) {
            return ResponseEntity.status(409).body(new ApiError(msg, 409));
        }

        if (msg != null && msg.contains("obligatori")) {
            return ResponseEntity.status(400).body(new ApiError(msg, 400));
        }

        return ResponseEntity.status(500).body(new ApiError("Error interno", 500));
    }
}
