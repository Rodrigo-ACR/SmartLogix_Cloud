package com.smartlogix.kafkaadmin.exception;

/** No hay respuesta del cluster Kafka dentro del tiempo maximo. */
public class KafkaNoDisponibleException extends RuntimeException {
    public KafkaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
