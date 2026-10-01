package com.smartlogix.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;

public class CrearBindingRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String exchange;

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String cola;

    // La routing key puede ser vacia solo en exchanges tipo fanout
    private String routingKey = "";

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getCola() { return cola; }
    public void setCola(String cola) { this.cola = cola; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
}
