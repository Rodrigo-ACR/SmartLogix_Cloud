package com.smartlogix.kafkaadmin.controller;

import com.smartlogix.kafkaadmin.service.KafkaAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ClusterController {

    private final KafkaAdminService service;

    public ClusterController(KafkaAdminService service) {
        this.service = service;
    }

    /** Brokers del cluster y cual es el controlador. */
    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> cluster() {
        return ResponseEntity.ok(service.cluster());
    }

    /** Foto completa: cluster + topicos + consumer groups. */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(service.overview());
    }
}
