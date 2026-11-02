package com.smartlogix.kafkaadmin.exception;

import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/** Traduce los errores de Kafka a respuestas HTTP con un mensaje claro. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, String>> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ResponseEntity<Map<String, String>> manejarProtegido(RecursoProtegidoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(KafkaNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> manejarNoDisponible(KafkaNoDisponibleException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(org.apache.kafka.common.errors.TimeoutException.class)
    public ResponseEntity<Map<String, String>> manejarTimeoutKafka(org.apache.kafka.common.errors.TimeoutException ex) {
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no respondio a tiempo: " + ex.getMessage());
    }

    @ExceptionHandler(TopicExistsException.class)
    public ResponseEntity<Map<String, String>> manejarTopicoExiste(TopicExistsException ex) {
        return responder(HttpStatus.CONFLICT, "El topico ya existe");
    }

    @ExceptionHandler(GroupNotEmptyException.class)
    public ResponseEntity<Map<String, String>> manejarGrupoActivo(GroupNotEmptyException ex) {
        return responder(HttpStatus.CONFLICT, "El consumer group tiene miembros activos; detenga los consumidores antes de eliminarlo");
    }

    @ExceptionHandler({UnknownTopicOrPartitionException.class, GroupIdNotFoundException.class})
    public ResponseEntity<Map<String, String>> manejarNoEncontrado(RuntimeException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado en Kafka: " + ex.getMessage());
    }

    @ExceptionHandler({InvalidTopicException.class, InvalidPartitionsException.class,
            InvalidReplicationFactorException.class, InvalidConfigurationException.class,
            InvalidRequestException.class})
    public ResponseEntity<Map<String, String>> manejarSolicitudInvalida(RuntimeException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Kafka rechazo la operacion: " + ex.getMessage());
    }
}
