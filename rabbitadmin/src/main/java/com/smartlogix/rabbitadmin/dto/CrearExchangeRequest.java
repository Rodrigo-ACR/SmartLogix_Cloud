package com.smartlogix.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CrearExchangeRequest {

    @NotBlank(message = "El nombre del exchange no puede estar vacio")
    @Pattern(regexp = "^[a-zA-Z0-9_.\\-]+$",
             message = "El nombre del exchange solo puede contener letras, numeros, puntos, guiones y guiones bajos")
    private String nombre;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(direct|topic|fanout|headers)$",
             message = "El tipo debe ser: direct, topic, fanout o headers")
    private String tipo;

    private boolean durable = true;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
}
