package com.smartlogix.pedidos.exception;

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

        if (msg != null && (msg.contains("obligatorio")
                || msg.contains("mayor a 0"))) {
            return ResponseEntity.status(400).body(new ApiError(msg, 400));
        }

        // Transiciones de estado inválidas: conflicto de negocio (TF-01)
        if (msg != null && msg.contains("no permitida")) {
            return ResponseEntity.status(409).body(new ApiError(msg, 409));
        }

        return ResponseEntity.status(500).body(new ApiError("Error interno", 500));
    }
}
