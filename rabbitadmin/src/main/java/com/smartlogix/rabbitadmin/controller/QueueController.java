package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.ActualizarColaRequest;
import com.smartlogix.rabbitadmin.dto.CrearColaRequest;
import com.smartlogix.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PutMapping("/{nombre}")
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable String nombre,
                                                          @RequestBody ActualizarColaRequest request,
                                                          @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(rabbitAdminService.actualizarCola(nombre, request, force));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String nombre,
                                                        @RequestParam(defaultValue = "false") boolean force) {
        rabbitAdminService.eliminarCola(nombre, force);
        return ResponseEntity.ok(Map.of("status", "eliminada", "nombre", nombre));
    }

    // Mira los primeros mensajes sin consumirlos (util para ver el contenido de una DLQ)
    @GetMapping("/{nombre}/mensajes")
    public ResponseEntity<List<Map<String, Object>>> verMensajes(@PathVariable String nombre,
                                                                 @RequestParam(defaultValue = "5") int cantidad) {
        return ResponseEntity.ok(rabbitAdminService.verMensajes(nombre, cantidad));
    }

    // Vacia la cola (no la elimina)
    @DeleteMapping("/{nombre}/mensajes")
    public ResponseEntity<Map<String, Object>> purgar(@PathVariable String nombre) {
        int eliminados = rabbitAdminService.purgarCola(nombre);
        return ResponseEntity.ok(Map.of("status", "purgada", "nombre", nombre, "mensajesEliminados", eliminados));
    }
}
