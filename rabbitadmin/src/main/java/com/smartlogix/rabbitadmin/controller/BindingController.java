package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.CrearBindingRequest;
import com.smartlogix.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bindings")
public class BindingController {

    private final RabbitAdminService rabbitAdminService;

    public BindingController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> crear(@Valid @RequestBody CrearBindingRequest request) {
        rabbitAdminService.crearBinding(request);
        return ResponseEntity.status(201).body(Map.of(
                "status", "creado",
                "exchange", request.getExchange(),
                "cola", request.getCola(),
                "routingKey", request.getRoutingKey()
        ));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> eliminar(@Valid @RequestBody CrearBindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
        return ResponseEntity.ok(Map.of("status", "eliminado"));
    }
}
