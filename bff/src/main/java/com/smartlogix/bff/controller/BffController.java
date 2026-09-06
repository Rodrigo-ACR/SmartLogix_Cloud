package com.smartlogix.bff.controller;

import com.smartlogix.bff.exception.ApiError;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class BffController {

        private static final Logger logger = LoggerFactory.getLogger(BffController.class);

        // RestTemplate @LoadBalanced inyectado desde RestTemplateConfig:
        // resuelve los nombres de servicio contra Eureka
        private final RestTemplate rest;

        public BffController(
                        @org.springframework.beans.factory.annotation.Qualifier("plainRestTemplate") RestTemplate rest) {
                this.rest = rest;
        }

        // =================================================
        // URLS MICROSERVICIOS
        // =================================================

        private final String URL_INVENTARIO = "http://inventario:8091/productos";
        private final String URL_PEDIDOS = "http://pedidos:8092/pedidos";
        private final String URL_ENVIOS = "http://envios:8093/envios";
        private final String URL_USUARIOS = "http://usuarios:8094/usuarios";

        // =================================================
        // INVENTARIO - GET
        // =================================================

        @GetMapping
        @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackInventario")
        public ResponseEntity<Object> obtenerProductos() {

                logger.info("GET /api/productos");

                Object respuesta = rest.getForObject(
                                URL_INVENTARIO,
                                Object.class);

                return ResponseEntity.ok(respuesta);
        }

        // =================================================
        // INVENTARIO - POST
        // =================================================

        @PostMapping
        @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackCrearProducto")
        public ResponseEntity<Object> crearProducto(
                        @RequestBody Object producto) {

                logger.info("POST /api/productos");

                Object respuesta = rest.postForObject(
                                URL_INVENTARIO,
                                producto,
                                Object.class);

                return ResponseEntity.status(201)
                                .body(respuesta);
        }

        // =================================================
        // INVENTARIO - PUT
        // =================================================

        @PutMapping("/{id}")
        @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackEditar")
        public ResponseEntity<?> editar(
                        @PathVariable Long id,
                        @RequestBody Object producto) {

                logger.info("PUT /api/productos/{}", id);

                rest.put(URL_INVENTARIO + "/" + id, producto);

                return ResponseEntity.ok("Actualizado");
        }

        // =================================================
        // INVENTARIO - DELETE
        // =================================================

        @DeleteMapping("/{id}")
        @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackEliminar")
        public ResponseEntity<?> eliminar(
                        @PathVariable Long id) {

                logger.info("DELETE /api/productos/{}", id);

                rest.delete(URL_INVENTARIO + "/" + id);

                return ResponseEntity.ok("Eliminado");
        }

        // =================================================
        // PEDIDOS - GET
        // =================================================

        @GetMapping("/pedidos")
        @CircuitBreaker(name = "pedidos", fallbackMethod = "fallbackPedidos")
        public ResponseEntity<Object> obtenerPedidos() {

                logger.info("GET /api/productos/pedidos");

                Object respuesta = rest.getForObject(
                                URL_PEDIDOS,
                                Object.class);

                return ResponseEntity.ok(respuesta);
        }

        // =================================================
        // PEDIDOS - POST
        // =================================================

        @PostMapping("/pedidos")
        @CircuitBreaker(name = "pedidos", fallbackMethod = "fallbackCrearPedido")
        public ResponseEntity<Object> crearPedido(
                        @RequestBody java.util.Map<String, Object> pedido) {

                logger.info("POST /api/productos/pedidos");

                Object respuesta = rest.postForObject(
                                URL_PEDIDOS,
                                pedido,
                                Object.class);

                // Descontar stock del producto en Inventario
                try {
                        Object productoId = pedido.get("productoId");
                        Object cantidad = pedido.get("cantidad");
                        if (productoId != null && cantidad != null) {
                                String urlDescontar = URL_INVENTARIO + "/" + productoId + "/descontar?cantidad="
                                                + cantidad;
                                org.springframework.http.HttpEntity<Void> req = new org.springframework.http.HttpEntity<>(
                                                null);
                                rest.put(urlDescontar, req);
                                logger.info("Stock descontado: producto={} cantidad={}", productoId, cantidad);
                        }
                } catch (Exception e) {
                        logger.warn("No se pudo descontar stock: {}", e.getMessage());
                }

                return ResponseEntity.status(201)
                                .body(respuesta);
        }

        // =================================================
        // PEDIDOS - PATCH estado
        // =================================================

        @PatchMapping("/pedidos/{id}/estado")
        @CircuitBreaker(name = "pedidos", fallbackMethod = "fallbackCambiarEstadoPedido")
        public ResponseEntity<Object> cambiarEstadoPedido(
                        @PathVariable Long id,
                        @RequestParam String estado) {

                logger.info("PATCH /api/productos/pedidos/{} estado={}", id, estado);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                org.springframework.web.client.RestTemplate rt = rest;
                ResponseEntity<Object> response = rt.exchange(
                                URL_PEDIDOS + "/" + id + "/estado?estado=" + estado,
                                org.springframework.http.HttpMethod.PUT,
                                request,
                                Object.class);

                return ResponseEntity.ok(response.getBody());
        }

        // =================================================
        // ENVIOS - PATCH estado
        // =================================================

        @PatchMapping("/envios/{id}/estado")
        @CircuitBreaker(name = "envios", fallbackMethod = "fallbackCambiarEstadoEnvio")
        public ResponseEntity<Object> cambiarEstadoEnvio(
                        @PathVariable Long id,
                        @RequestParam String estado) {

                logger.info("PATCH /api/productos/envios/{} estado={}", id, estado);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                ResponseEntity<Object> response = rest.exchange(
                                URL_ENVIOS + "/" + id + "/estado?estado=" + estado,
                                org.springframework.http.HttpMethod.PUT,
                                request,
                                Object.class);

                return ResponseEntity.ok(response.getBody());
        }

        // =================================================
        // ENVIOS - GET
        // =================================================

        @GetMapping("/envios")
        @CircuitBreaker(name = "envios", fallbackMethod = "fallbackEnvios")
        public ResponseEntity<Object> obtenerEnvios() {

                logger.info("GET /api/productos/envios");

                Object respuesta = rest.getForObject(
                                URL_ENVIOS,
                                Object.class);

                return ResponseEntity.ok(respuesta);
        }

        // =================================================
        // ENVIOS - POST
        // =================================================

        @PostMapping("/envios")
        @CircuitBreaker(name = "envios", fallbackMethod = "fallbackCrearEnvio")
        public ResponseEntity<Object> crearEnvio(@RequestBody Object body) {
                logger.info("POST /api/productos/envios");
                Object respuesta = rest.postForObject(URL_ENVIOS, body, Object.class);
                return ResponseEntity.status(201).body(respuesta);
        }

        // =================================================
        // USUARIOS - REGISTER
        // =================================================

        @PostMapping("/usuarios/register")
        @CircuitBreaker(name = "usuarios", fallbackMethod = "fallbackUsuarios")
        public ResponseEntity<Object> register(
                        @RequestBody Object body) {

                logger.info("POST /api/productos/usuarios/register");

                try {
                        Object respuesta = rest.postForObject(
                                        URL_USUARIOS + "/register",
                                        body,
                                        Object.class);

                        return ResponseEntity.status(201).body(respuesta);

                } catch (org.springframework.web.client.HttpClientErrorException e) {
                        // Error de negocio del MS (400, 404, etc): reenviar tal cual al frontend
                        return ResponseEntity.status(e.getStatusCode())
                                        .body(e.getResponseBodyAs(Object.class));
                }
        }

        // =================================================
        // USUARIOS - GET (admin)
        // =================================================

        @GetMapping("/usuarios")
        @CircuitBreaker(name = "usuarios", fallbackMethod = "fallbackUsuarios")
        public ResponseEntity<Object> getUsuarios() {

                logger.info("GET /api/productos/usuarios");

                Object respuesta = rest.getForObject(
                                URL_USUARIOS,
                                Object.class);

                return ResponseEntity.ok(respuesta);
        }

        // =================================================
        // USUARIOS - PUT (admin)
        // =================================================

        @PutMapping("/usuarios/{id}")
        @CircuitBreaker(name = "usuarios", fallbackMethod = "fallbackUsuariosConId")
        public ResponseEntity<Object> actualizarUsuario(
                        @PathVariable Long id,
                        @RequestBody Object body) {

                logger.info("PUT /api/productos/usuarios/{}", id);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Object> request = new HttpEntity<>(body, headers);

                rest.put(URL_USUARIOS + "/" + id, request);

                return ResponseEntity.ok("{\"mensaje\": \"Usuario actualizado\"}");
        }

        // =================================================
        // FALLBACK INVENTARIO
        // =================================================

        public ResponseEntity<Object> fallbackInventario(
                        Throwable e) {

                logger.error("CircuitBreaker INVENTARIO: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "Inventario temporalmente no disponible",
                                                503));
        }

        public ResponseEntity<Object> fallbackCrearProducto(
                        Object producto,
                        Throwable e) {

                logger.error("CircuitBreaker CREAR PRODUCTO: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "No se pudo crear producto",
                                                503));
        }

        public ResponseEntity<?> fallbackEditar(
                        Long id,
                        Object producto,
                        Throwable e) {

                logger.error("CircuitBreaker EDITAR: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "No se pudo editar producto",
                                                503));
        }

        public ResponseEntity<?> fallbackEliminar(
                        Long id,
                        Throwable e) {

                logger.error("CircuitBreaker ELIMINAR: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "No se pudo eliminar producto",
                                                503));
        }

        // =================================================
        // FALLBACK PEDIDOS
        // =================================================

        public ResponseEntity<Object> fallbackPedidos(
                        Throwable e) {

                logger.error("CircuitBreaker PEDIDOS: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "Pedidos temporalmente no disponibles",
                                                503));
        }

        public ResponseEntity<Object> fallbackCrearPedido(
                        Object pedido,
                        Throwable e) {

                logger.error("CircuitBreaker CREAR PEDIDO: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "No se pudo crear pedido",
                                                503));
        }

        public ResponseEntity<Object> fallbackCambiarEstadoPedido(
                        Long id, String estado, Throwable e) {
                logger.error("CircuitBreaker CAMBIAR ESTADO PEDIDO: {}", e.getMessage());
                return ResponseEntity.status(503)
                                .body(new ApiError("No se pudo cambiar estado del pedido", 503));
        }

        public ResponseEntity<Object> fallbackCambiarEstadoEnvio(
                        Long id, String estado, Throwable e) {
                logger.error("CircuitBreaker CAMBIAR ESTADO ENVIO: {}", e.getMessage());
                return ResponseEntity.status(503)
                                .body(new ApiError("No se pudo cambiar estado del envio", 503));
        }

        // =================================================
        // FALLBACK ENVIOS
        // =================================================

        public ResponseEntity<Object> fallbackEnvios(
                        Throwable e) {

                logger.error("CircuitBreaker ENVIOS: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "Envios temporalmente no disponibles",
                                                503));
        }

        // =================================================
        // FALLBACK USUARIOS
        // =================================================

        public ResponseEntity<Object> fallbackUsuarios(
                        Throwable e) {

                logger.error("CircuitBreaker USUARIOS: {}", e.getMessage());

                return ResponseEntity.status(503)
                                .body(new ApiError(
                                                "Usuarios temporalmente no disponible",
                                                503));
        }

        // =================================================
        // FALLBACK CREAR ENVIO
        // =================================================

        public ResponseEntity<Object> fallbackCrearEnvio(Object body, Throwable e) {
                logger.error("CircuitBreaker CREAR ENVIO: {}", e.getMessage());
                return ResponseEntity.status(503)
                                .body(new ApiError("No se pudo crear envio", 503));
        }

        // =================================================
        // FALLBACK USUARIOS CON ID
        // =================================================

        public ResponseEntity<Object> fallbackUsuariosConId(
                        Long id, Object body, Throwable e) {
                logger.error("CircuitBreaker USUARIOS PUT: {}", e.getMessage());
                return ResponseEntity.status(503)
                                .body(new ApiError("Usuarios temporalmente no disponible", 503));
        }
}