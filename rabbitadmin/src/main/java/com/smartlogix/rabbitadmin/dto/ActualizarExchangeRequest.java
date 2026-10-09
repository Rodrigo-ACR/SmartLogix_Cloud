package com.smartlogix.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para actualizar un exchange (tipo y durabilidad). RabbitMQ no permite
 * cambiar el tipo de un exchange existente, asi que se elimina y se recrea.
 */
public class ActualizarExchangeRequest {

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(direct|topic|fanout|headers)$",
             message = "El tipo debe ser: direct, topic, fanout o headers")
    private String tipo;

    private boolean durable = true;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
}
