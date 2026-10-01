package com.smartlogix.rabbitadmin.service;

import com.smartlogix.rabbitadmin.dto.CrearBindingRequest;
import com.smartlogix.rabbitadmin.dto.CrearColaRequest;
import com.smartlogix.rabbitadmin.dto.CrearExchangeRequest;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsula toda la logica de creacion, consulta y eliminacion de
 * colas, exchanges y bindings en RabbitMQ. Los controladores solo
 * llaman a metodos de alto nivel de este servicio y no conocen
 * detalles internos de la libreria de RabbitMQ (AmqpAdmin).
 */
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    // ================= COLAS =================

    public void crearCola(CrearColaRequest request) {
        Map<String, Object> args = new HashMap<>();
        if (request.getDeadLetterExchange() != null && !request.getDeadLetterExchange().isBlank()) {
            args.put("x-dead-letter-exchange", request.getDeadLetterExchange());
        }
        Queue queue = new Queue(request.getNombre(), request.isDurable(), false, false, args);
        amqpAdmin.declareQueue(queue);
    }

    public void eliminarCola(String nombre) {
        boolean eliminada = amqpAdmin.deleteQueue(nombre);
        if (!eliminada) {
            throw new IllegalArgumentException("No se encontro la cola: " + nombre);
        }
    }

    public Properties obtenerInfoCola(String nombre) {
        Properties props = amqpAdmin.getQueueProperties(nombre);
        if (props == null) {
            throw new IllegalArgumentException("No se encontro la cola: " + nombre);
        }
        return props;
    }

    // ================= EXCHANGES =================

    public void crearExchange(CrearExchangeRequest request) {
        Exchange exchange = switch (request.getTipo()) {
            case "direct" -> new DirectExchange(request.getNombre(), request.isDurable(), false);
            case "topic" -> new TopicExchange(request.getNombre(), request.isDurable(), false);
            case "fanout" -> new FanoutExchange(request.getNombre(), request.isDurable(), false);
            case "headers" -> new HeadersExchange(request.getNombre(), request.isDurable(), false);
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + request.getTipo());
        };
        amqpAdmin.declareExchange(exchange);
    }

    public void eliminarExchange(String nombre) {
        boolean eliminado = amqpAdmin.deleteExchange(nombre);
        if (!eliminado) {
            throw new IllegalArgumentException("No se encontro el exchange: " + nombre);
        }
    }

    // ================= BINDINGS =================

    public void crearBinding(CrearBindingRequest request) {
        Binding binding = new Binding(
                request.getCola(),
                Binding.DestinationType.QUEUE,
                request.getExchange(),
                request.getRoutingKey(),
                null
        );
        amqpAdmin.declareBinding(binding);
    }

    public void eliminarBinding(CrearBindingRequest request) {
        Binding binding = new Binding(
                request.getCola(),
                Binding.DestinationType.QUEUE,
                request.getExchange(),
                request.getRoutingKey(),
                null
        );
        amqpAdmin.removeBinding(binding);
    }
}
