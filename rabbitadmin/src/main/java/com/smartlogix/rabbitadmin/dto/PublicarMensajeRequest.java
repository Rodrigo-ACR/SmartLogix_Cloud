package com.smartlogix.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para publicar un mensaje de prueba en un exchange. Sirve para probar
 * los flujos completos (productor -> exchange -> cola -> consumidor) y para
 * provocar a proposito un rechazo (NACK) y ver el mensaje llegar a la DLQ.
 */
public class PublicarMensajeRequest {

    // Vacio = exchange por defecto de RabbitMQ (envia directo a la cola con el nombre de la routing key)
    @NotNull(message = "El exchange es obligatorio (puede ser vacio para el exchange por defecto)")
    @Pattern(regexp = "^[a-zA-Z0-9_.\\-]*$", message = "Nombre de exchange invalido")
    private String exchange = "";

    private String routingKey = "";

    @NotBlank(message = "El mensaje no puede estar vacio")
    private String mensaje;

    @Pattern(regexp = "^[a-zA-Z0-9/+.\\-]+$", message = "Content-Type invalido")
    private String contentType = "application/json";

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
