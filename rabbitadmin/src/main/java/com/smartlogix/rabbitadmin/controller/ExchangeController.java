package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.CrearExchangeRequest;
import com.smartlogix.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/exchanges")
public class ExchangeController {

    private final RabbitAdminService rabbitAdminService;

    public ExchangeController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> crear(@Valid @RequestBody CrearExchangeRequest request) {
        rabbitAdminService.crearExchange(request);
        return ResponseEntity.status(201).body(Map.of(
                "status", "creado",
                "nombre", request.getNombre(),
                "tipo", request.getTipo()
        ));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String nombre) {
        rabbitAdminService.eliminarExchange(nombre);
        return ResponseEntity.ok(Map.of("status", "eliminado", "nombre", nombre));
    }
}
