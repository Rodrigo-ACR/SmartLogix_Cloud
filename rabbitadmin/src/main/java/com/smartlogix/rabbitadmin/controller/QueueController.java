package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.CrearColaRequest;
import com.smartlogix.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/queues")
public class QueueController {

    private final RabbitAdminService rabbitAdminService;

    public QueueController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> crear(@Valid @RequestBody CrearColaRequest request) {
        rabbitAdminService.crearCola(request);
        return ResponseEntity.status(201).body(Map.of(
                "status", "creada",
                "nombre", request.getNombre()
        ));
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<Properties> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok(rabbitAdminService.obtenerInfoCola(nombre));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String nombre) {
        rabbitAdminService.eliminarCola(nombre);
        return ResponseEntity.ok(Map.of("status", "eliminada", "nombre", nombre));
    }
}
