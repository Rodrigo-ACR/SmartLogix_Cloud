package com.smartlogix.rabbitadmin.exception;

/**
 * Se lanza cuando no se puede consultar la API de management de RabbitMQ.
 */
public class RabbitNoDisponibleException extends RuntimeException {
    public RabbitNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
