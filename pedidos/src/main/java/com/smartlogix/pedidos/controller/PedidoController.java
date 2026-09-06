package com.smartlogix.pedidos.controller;

import com.smartlogix.pedidos.exception.ApiError;
import com.smartlogix.pedidos.model.EstadoPedido;
import com.smartlogix.pedidos.model.Pedido;
import com.smartlogix.pedidos.service.PedidoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    private static final Logger logger = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Pedido> listar() {
        logger.info("GET /pedidos");
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtener(@PathVariable Long id) {
        logger.info("GET /pedidos/{}", id);
        return service.buscarPorId(id)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.status(404)
                        .body(new ApiError("Pedido no encontrado", 404)));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody Pedido p) {
        logger.info("POST /pedidos");
        Pedido nuevo = service.crear(p);
        return ResponseEntity.status(201).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id,
            @RequestBody Pedido pedido) {
        logger.info("PUT /pedidos/{}", id);
        return ResponseEntity.ok(service.actualizar(id, pedido));
    }

    // Endpoint para cambiar estado (PATCH original + PUT para compatibilidad con RestTemplate)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(@PathVariable Long id,
            @RequestParam EstadoPedido estado) {
        logger.info("PATCH /pedidos/{}/estado -> {}", id, estado);
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstadoPut(@PathVariable Long id,
            @RequestParam EstadoPedido estado) {
        logger.info("PUT /pedidos/{}/estado -> {}", id, estado);
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        logger.info("DELETE /pedidos/{}", id);
        service.eliminar(id);
        return ResponseEntity.ok("Pedido eliminado");
    }
}