package com.smartlogix.bff.controller;

import com.smartlogix.bff.exception.ApiError;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Puerta de entrada del frontend hacia el microservicio kafkaadmin (EV3).
 *
 * kafkaadmin no valida tokens, asi que el frontend nunca le habla directo:
 * todo pasa por el BFF, que exige el JWT (Azure AD o local) igual que con el
 * resto de los microservicios, y reenvia la peticion tal cual (metodo, query,
 * cuerpo) devolviendo el mismo status y cuerpo que entrega kafkaadmin.
 *
 * /api/kafka/overview  ->  http://kafkaadmin:8096/overview
 */
@RestController
@RequestMapping("/api/kafka")
public class KafkaProxyController {

        private static final Logger logger = LoggerFactory.getLogger(KafkaProxyController.class);

        private static final String URL_KAFKAADMIN = "http://kafkaadmin:8096";
        private static final String PREFIJO = "/api/kafka";

        // Solo se reenvian los recursos de administracion; nada de actuator u otras rutas
        private static final String RUTAS_PERMITIDAS = "^/(cluster|topics|groups|overview)(/[A-Za-z0-9_.\\-]+)*/?$";

        private final RestTemplate rest;

        public KafkaProxyController(@Qualifier("plainRestTemplate") RestTemplate rest) {
                this.rest = rest;
        }

        @RequestMapping(value = "/**", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                        RequestMethod.DELETE })
        @CircuitBreaker(name = "kafkaadmin", fallbackMethod = "fallbackKafka")
        public ResponseEntity<Object> reenviar(HttpServletRequest request,
                        @RequestBody(required = false) byte[] cuerpo) {

                String ruta = request.getRequestURI().substring(PREFIJO.length());

                if (!ruta.matches(RUTAS_PERMITIDAS)) {
                        return ResponseEntity.status(404)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body("{\"mensaje\":\"Ruta no permitida\"}".getBytes());
                }

                String query = request.getQueryString();
                URI uri = URI.create(URL_KAFKAADMIN + ruta + (query != null ? "?" + query : ""));

                HttpHeaders headers = new HttpHeaders();
                if (request.getContentType() != null) {
                        headers.set(HttpHeaders.CONTENT_TYPE, request.getContentType());
                }

                logger.info("{} /api/kafka{}", request.getMethod(), ruta);

                try {
                        ResponseEntity<byte[]> respuesta = rest.exchange(
                                        uri,
                                        HttpMethod.valueOf(request.getMethod()),
                                        new HttpEntity<>(cuerpo, headers),
                                        byte[].class);

                        return ResponseEntity.status(respuesta.getStatusCode().value())
                                        .contentType(contentTypeOrJson(respuesta.getHeaders()))
                                        .body(respuesta.getBody());

                } catch (HttpStatusCodeException e) {
                        // Error de negocio de kafkaadmin (400, 404, 409...): se devuelve tal cual al frontend
                        return ResponseEntity.status(e.getStatusCode().value())
                                        .contentType(contentTypeOrJson(e.getResponseHeaders()))
                                        .body(e.getResponseBodyAsByteArray());
                }
        }

        private MediaType contentTypeOrJson(HttpHeaders headers) {
                MediaType tipo = headers != null ? headers.getContentType() : null;
                return tipo != null ? tipo : MediaType.APPLICATION_JSON;
        }

        public ResponseEntity<Object> fallbackKafka(HttpServletRequest request, byte[] cuerpo, Throwable e) {
                logger.error("CircuitBreaker KAFKAADMIN: {}", e.getMessage());
                return ResponseEntity.status(503)
                                .body(new ApiError("kafkaadmin temporalmente no disponible", 503));
        }
}
