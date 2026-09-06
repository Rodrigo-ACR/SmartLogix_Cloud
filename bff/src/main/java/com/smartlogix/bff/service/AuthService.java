package com.smartlogix.bff.service;

import com.smartlogix.bff.model.Usuario;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

@Service
public class AuthService {

    // RestTemplate SIN LoadBalancer — llama directo al contenedor Docker
    private final RestTemplate rest;

    public AuthService(@Qualifier("plainRestTemplate") RestTemplate rest) {
        this.rest = rest;
    }

    // Nombre del contenedor Docker + puerto interno
    private final String URL_USUARIOS = "http://usuarios:8094/usuarios";

    public Usuario login(String correo, String password) {

        Map<String, String> body = Map.of(
                "correo", correo,
                "password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Usuario> response = rest.postForEntity(
                    URL_USUARIOS + "/login",
                    request,
                    Usuario.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[AuthService] Error en login: " + e.getMessage());
            return null;
        }
    }
}