package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.PublicarMensajeRequest;
import com.smartlogix.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/publish")
public class PublishController {

    private final RabbitAdminService rabbitAdminService;

    public PublishController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> publicar(@Valid @RequestBody PublicarMensajeRequest request) {
        rabbitAdminService.publicar(request);
        return ResponseEntity.status(202).body(Map.of(
                "status", "publicado",
                "exchange", request.getExchange(),
                "routingKey", request.getRoutingKey()
        ));
    }
}
