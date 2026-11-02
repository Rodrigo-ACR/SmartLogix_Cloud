package com.smartlogix.kafkaadmin.controller;

import com.smartlogix.kafkaadmin.dto.ActualizarConfigRequest;
import com.smartlogix.kafkaadmin.dto.AumentarParticionesRequest;
import com.smartlogix.kafkaadmin.dto.CrearTopicoRequest;
import com.smartlogix.kafkaadmin.dto.PublicarMensajeRequest;
import com.smartlogix.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/topics")
public class TopicController {

    private final KafkaAdminService service;

    public TopicController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(service.listarTopicos());
    }

    /** Particiones (lider, replicas, ISR, offsets) y configuracion (retencion, cleanup policy...). */
    @GetMapping("/{nombre}")
    public ResponseEntity<Map<String, Object>> describir(@PathVariable String nombre) {
        return ResponseEntity.ok(service.describirTopico(nombre));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody CrearTopicoRequest request) {
        service.crearTopico(request);
        return ResponseEntity.status(201).body(Map.of(
                "status", "creado",
                "nombre", request.getNombre(),
                "particiones", request.getParticiones(),
                "replicas", request.getReplicas()));
    }

    /** Modifica la configuracion (retention.ms, cleanup.policy, min.insync.replicas...). */
    @PutMapping("/{nombre}/config")
    public ResponseEntity<Map<String, Object>> actualizarConfig(@PathVariable String nombre,
                                                                @Valid @RequestBody ActualizarConfigRequest request) {
        return ResponseEntity.ok(service.actualizarConfig(nombre, request.getConfigs()));
    }

    /** Aumenta el numero de particiones (Kafka no permite reducirlas). */
    @PutMapping("/{nombre}/particiones")
    public ResponseEntity<Map<String, Object>> aumentarParticiones(@PathVariable String nombre,
                                                                   @Valid @RequestBody AumentarParticionesRequest request) {
        service.aumentarParticiones(nombre, request.getTotal());
        return ResponseEntity.ok(Map.of("status", "particiones aumentadas", "nombre", nombre, "total", request.getTotal()));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String nombre,
                                                        @RequestParam(defaultValue = "false") boolean force) {
        service.eliminarTopico(nombre, force);
        return ResponseEntity.ok(Map.of("status", "eliminado", "nombre", nombre));
    }

    /** Ultimos mensajes del topico (sin consumirlos). Util para ver el contenido de un DLT. */
    @GetMapping("/{nombre}/mensajes")
    public ResponseEntity<List<Map<String, Object>>> verMensajes(@PathVariable String nombre,
                                                                 @RequestParam(defaultValue = "5") int cantidad,
                                                                 @RequestParam(required = false) Integer particion) {
        return ResponseEntity.ok(service.verMensajes(nombre, cantidad, particion));
    }

    /** Publica un mensaje de prueba en el topico. */
    @PostMapping("/{nombre}/mensajes")
    public ResponseEntity<Map<String, Object>> publicar(@PathVariable String nombre,
                                                        @Valid @RequestBody PublicarMensajeRequest request) {
        return ResponseEntity.status(201).body(service.publicar(nombre, request.getClave(), request.getValor()));
    }
}
