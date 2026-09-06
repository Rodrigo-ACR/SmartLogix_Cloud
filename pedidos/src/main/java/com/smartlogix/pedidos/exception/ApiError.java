package com.smartlogix.pedidos.exception;

import java.time.LocalDate;

public class ApiError {

    private String mensaje;
    private int codigo;
    private LocalDate fecha;

    public ApiError(String mensaje, int codigo) {
        this.mensaje = mensaje;
        this.codigo = codigo;
        this.fecha = LocalDate.now();
    }

    public String getMensaje() { return mensaje; }
    public int getCodigo() { return codigo; }
    public LocalDate getFecha() { return fecha; }
}