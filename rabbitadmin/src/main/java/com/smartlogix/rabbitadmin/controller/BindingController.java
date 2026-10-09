package com.smartlogix.rabbitadmin.controller;

import com.smartlogix.rabbitadmin.dto.ActualizarBindingRequest;
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

    // Cambia la routing key de un binding existente
    @PutMapping
    public ResponseEntity<Map<String, String>> actualizar(@Valid @RequestBody ActualizarBindingRequest request,
                                                          @RequestParam(defaultValue = "false") boolean force) {
        rabbitAdminService.actualizarBinding(request, force);
        return ResponseEntity.ok(Map.of(
                "status", "actualizado",
                "exchange", request.getExchange(),
                "cola", request.getCola(),
                "routingKey", request.getNuevaRoutingKey()
        ));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> eliminar(@Valid @RequestBody CrearBindingRequest request,
                                                        @RequestParam(defaultValue = "false") boolean force) {
        rabbitAdminService.eliminarBinding(request, force);
        return ResponseEntity.ok(Map.of("status", "eliminado"));
    }

    // Misma operacion pero con parametros en la URL (sin cuerpo). Lo usa el frontend a traves del BFF,
    // porque un DELETE con cuerpo no siempre lo soportan los clientes HTTP intermedios.
    @DeleteMapping(params = { "exchange", "cola" })
    public ResponseEntity<Map<String, String>> eliminarPorParametros(@RequestParam String exchange,
                                                                     @RequestParam String cola,
                                                                     @RequestParam(defaultValue = "") String routingKey,
                                                                     @RequestParam(defaultValue = "false") boolean force) {
        CrearBindingRequest request = new CrearBindingRequest();
        request.setExchange(exchange);
        request.setCola(cola);
        request.setRoutingKey(routingKey);
        rabbitAdminService.eliminarBinding(request, force);
        return ResponseEntity.ok(Map.of("status", "eliminado"));
    }
}
