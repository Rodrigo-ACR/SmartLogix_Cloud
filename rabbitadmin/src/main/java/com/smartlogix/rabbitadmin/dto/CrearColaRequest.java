package com.smartlogix.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de entrada para crear una cola.
 * Las anotaciones de validacion evitan que se creen colas con
 * nombres vacios o con caracteres no permitidos por RabbitMQ.
 */
public class CrearColaRequest {

    @NotBlank(message = "El nombre de la cola no puede estar vacio")
    @Pattern(regexp = "^[a-zA-Z0-9_.\\-]+$",
             message = "El nombre de la cola solo puede contener letras, numeros, puntos, guiones y guiones bajos")
    private String nombre;

    private boolean durable = true;

    // Opcional: si se especifica, la cola se crea con Dead Letter Exchange
    private String deadLetterExchange;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }

    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }
}
