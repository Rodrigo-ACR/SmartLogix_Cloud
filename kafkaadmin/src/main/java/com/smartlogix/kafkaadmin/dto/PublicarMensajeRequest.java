package com.smartlogix.kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;

/** Mensaje de prueba a publicar en un topico (clave opcional, valor obligatorio). */
public class PublicarMensajeRequest {

    private String clave;

    @NotBlank(message = "El valor del mensaje no puede estar vacio")
    private String valor;

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
