package com.smartlogix.envios.controller;

import com.smartlogix.envios.exception.ApiError;
import com.smartlogix.envios.model.EstadoEnvio;
import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.service.EnvioService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/envios")
@CrossOrigin(origins = "*")
public class EnvioController {

    private static final Logger logger = LoggerFactory.getLogger(EnvioController.class);

    private final EnvioService service;

    public EnvioController(EnvioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Envio> listar() {
        logger.info("GET /envios");
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtener(@PathVariable Long id) {
        logger.info("GET /envios/{}", id);
        return service.buscarPorId(id)
                .map(e -> ResponseEntity.ok((Object) e))
                .orElse(ResponseEntity.status(404)
                        .body(new ApiError("Envio no encontrado", 404)));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody Envio e) {
        logger.info("POST /envios");
        Envio nuevo = service.crear(e);
        return ResponseEntity.status(201).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id,
            @RequestBody Envio envio) {
        logger.info("PUT /envios/{}", id);
        return ResponseEntity.ok(service.actualizar(id, envio));
    }

    // Endpoint para cambiar estado (PATCH original + PUT para compatibilidad con
    // RestTemplate)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable Long id,
            @RequestParam EstadoEnvio estado) {
        logger.info("PATCH /envios/{}/estado -> {}", id, estado);
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstadoPut(@PathVariable Long id,
            @RequestParam EstadoEnvio estado) {
        logger.info("PUT /envios/{}/estado -> {}", id, estado);
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        logger.info("DELETE /envios/{}", id);
        service.eliminar(id);
        return ResponseEntity.ok("Envio eliminado");
    }
}