package com.smartlogix.usuarios.exception;

public class ApiError {

    private String mensaje;
    private int codigo;

    public ApiError(String mensaje, int codigo) {
        this.mensaje = mensaje;
        this.codigo = codigo;
    }

    public String getMensaje() { return mensaje; }
    public int getCodigo() { return codigo; }
}