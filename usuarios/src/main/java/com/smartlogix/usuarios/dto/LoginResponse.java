package com.smartlogix.usuarios.dto;

public class LoginResponse {

    private Long id;
    private String nombre;
    private String correo;
    private String rol;
    private String direccion;
    private String telefono;

    public LoginResponse(Long id,
            String nombre,
            String correo,
            String rol,
            String direccion,
            String telefono) {

        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }
}