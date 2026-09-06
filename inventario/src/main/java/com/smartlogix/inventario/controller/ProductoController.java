package com.smartlogix.inventario.controller;

import com.smartlogix.inventario.exception.ApiError;
import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.service.ProductoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productos")
@CrossOrigin("*")
public class ProductoController {

    private static final Logger logger = LoggerFactory.getLogger(ProductoController.class);

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        logger.info("GET /productos");
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtener(@PathVariable Long id) {
        logger.info("GET /productos/{}", id);
        return service.buscarPorId(id)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.status(404)
                        .body(new ApiError("Producto no encontrado", 404)));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody Producto producto) {
        logger.info("POST /productos");
        Producto nuevo = service.crear(producto);
        return ResponseEntity.status(201).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Long id,
            @RequestBody Producto producto) {
        logger.info("PUT /productos/{}", id);
        Producto actualizado = service.actualizar(id, producto);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Long id) {
        logger.info("DELETE /productos/{}", id);
        service.eliminar(id);
        return ResponseEntity.ok("Producto eliminado");
    }

    // Endpoint para que Pedidos descuente stock
    @PutMapping("/{id}/descontar")
    public ResponseEntity<Object> descontarStock(@PathVariable Long id,
            @RequestParam int cantidad) {
        logger.info("PUT /productos/{}/descontar cantidad={}", id, cantidad);
        service.descontarStock(id, cantidad);
        return ResponseEntity.ok("Stock actualizado");
    }
}