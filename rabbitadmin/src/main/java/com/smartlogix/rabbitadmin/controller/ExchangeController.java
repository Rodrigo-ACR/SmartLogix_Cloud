package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.ActualizarExchangeRequest;
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

    @PutMapping("/{nombre}")
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable String nombre,
                                                          @Valid @RequestBody ActualizarExchangeRequest request,
                                                          @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(rabbitAdminService.actualizarExchange(nombre, request, force));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String nombre,
                                                        @RequestParam(defaultValue = "false") boolean force) {
        rabbitAdminService.eliminarExchange(nombre, force);
        return ResponseEntity.ok(Map.of("status", "eliminado", "nombre", nombre));
    }
}
