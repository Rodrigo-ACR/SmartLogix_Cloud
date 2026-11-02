package com.smartlogix.kafkaadmin.controller;

import com.smartlogix.kafkaadmin.service.KafkaAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/groups")
public class ConsumerGroupController {

    private final KafkaAdminService service;

    public ConsumerGroupController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(service.listarGrupos());
    }

    /** Miembros, particiones asignadas, offsets y lag del grupo. */
    @GetMapping("/{grupo}")
    public ResponseEntity<Map<String, Object>> describir(@PathVariable String grupo) {
        return ResponseEntity.ok(service.describirGrupo(grupo));
    }

    /** Elimina el grupo (solo si no tiene consumidores conectados). */
    @DeleteMapping("/{grupo}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable String grupo) {
        service.eliminarGrupo(grupo);
        return ResponseEntity.ok(Map.of("status", "eliminado", "grupo", grupo));
    }
}
