package com.smartlogix.kafkaadmin.exception;

/** Se intento modificar un recurso del sistema sin confirmar con force=true. */
public class RecursoProtegidoException extends RuntimeException {
    public RecursoProtegidoException(String mensaje) {
        super(mensaje);
    }
}
