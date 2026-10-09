package com.smartlogix.rabbitadmin.dto;

/**
 * DTO para actualizar una cola. RabbitMQ no permite cambiar los argumentos de
 * una cola existente, por eso la actualizacion elimina y vuelve a crear la cola.
 */
public class ActualizarColaRequest {

    private boolean durable = true;

    // Opcional: si viene vacio, la cola queda sin Dead Letter Exchange
    private String deadLetterExchange;

    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }

    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }
}
