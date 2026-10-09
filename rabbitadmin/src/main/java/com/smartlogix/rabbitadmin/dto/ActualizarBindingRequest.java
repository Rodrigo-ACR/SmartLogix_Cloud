package com.smartlogix.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para cambiar la routing key de un binding existente.
 */
public class ActualizarBindingRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String exchange;

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String cola;

    // Routing key actual del binding (puede ser vacia en fanout)
    private String routingKey = "";

    // Routing key nueva
    private String nuevaRoutingKey = "";

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getCola() { return cola; }
    public void setCola(String cola) { this.cola = cola; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public String getNuevaRoutingKey() { return nuevaRoutingKey; }
    public void setNuevaRoutingKey(String nuevaRoutingKey) { this.nuevaRoutingKey = nuevaRoutingKey; }
}
