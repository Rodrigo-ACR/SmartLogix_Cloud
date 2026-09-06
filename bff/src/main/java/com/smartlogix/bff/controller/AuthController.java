package com.smartlogix.bff.controller;

import com.smartlogix.bff.model.Usuario;
import com.smartlogix.bff.service.AuthService;
import com.smartlogix.bff.security.JwtUtil;
import com.smartlogix.bff.exception.ApiError;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class AuthController {

    private final AuthService service;
    private final JwtUtil jwt;

    public AuthController(AuthService service, JwtUtil jwt) {
        this.service = service;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario req) {

        Usuario user = service.login(req.getCorreo(), req.getPassword());

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(new ApiError("Credenciales incorrectas", 401));
        }

        String token = jwt.generarToken(user);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "rol", user.getRol(),
                "nombre", user.getNombre(),
                "id", user.getId(),
                "telefono", user.getTelefono() != null ? user.getTelefono() : "",
                "direccion", user.getDireccion() != null ? user.getDireccion() : ""));
    }
}