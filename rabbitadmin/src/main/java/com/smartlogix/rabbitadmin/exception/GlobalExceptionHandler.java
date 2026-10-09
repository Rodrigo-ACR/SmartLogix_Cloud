package com.smartlogix.rabbitadmin.exception;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern REPLY_TEXT = Pattern.compile("reply-text=(.+?), class-id");

    // Errores de validacion de los DTO (@NotBlank, @Pattern, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errores);
    }

    // JSON mal formado o ausente
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El cuerpo de la peticion no es un JSON valido"));
    }

    // Errores de negocio (cola/exchange no encontrado, tipo invalido, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", ex.getMessage()));
    }

    // Recurso de la topologia de un microservicio: no se toca sin force=true
    @ExceptionHandler(RecursoProtegidoException.class)
    public ResponseEntity<Map<String, String>> manejarProtegido(RecursoProtegidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", ex.getMessage()));
    }

    // No hay conexion con la API de management
    @ExceptionHandler(RabbitNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> manejarNoDisponible(RabbitNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("mensaje", ex.getMessage()));
    }

    // No hay conexion AMQP con el broker
    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Map<String, String>> manejarSinConexion(AmqpConnectException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("mensaje", "No hay conexion con RabbitMQ"));
    }

    // Rechazos del broker: PRECONDITION_FAILED (misma cola con otros argumentos), NOT_FOUND (exchange o cola
    // inexistente al crear un binding), etc.
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, String>> manejarAmqp(AmqpException ex) {
        Throwable raiz = ex;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String causa = String.valueOf(raiz.getMessage());
        Matcher m = REPLY_TEXT.matcher(causa);
        String detalle = m.find() ? m.group(1).trim() : causa;
        HttpStatus status = causa.contains("NOT_FOUND") ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(Map.of("mensaje", "RabbitMQ rechazo la operacion: " + detalle));
    }
}
