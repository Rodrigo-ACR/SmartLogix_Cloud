package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.service.RabbitManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Foto completa de la topologia del broker (nodos, colas, exchanges y
 * bindings). La consume la vista "Demo EP2" del frontend.
 */
@RestController
@RequestMapping("/overview")
public class OverviewController {

    private final RabbitManagementService management;

    public OverviewController(RabbitManagementService management) {
        this.management = management;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(management.overview());
    }
}
