package com.smartlogix.rabbitadmin.exception;

/**
 * Se lanza cuando se intenta modificar o eliminar un recurso que forma parte
 * de la topologia de los microservicios (colas, exchanges y bindings que los
 * propios microservicios declaran al arrancar).
 */
public class RecursoProtegidoException extends RuntimeException {
    public RecursoProtegidoException(String mensaje) {
        super(mensaje);
    }
}
