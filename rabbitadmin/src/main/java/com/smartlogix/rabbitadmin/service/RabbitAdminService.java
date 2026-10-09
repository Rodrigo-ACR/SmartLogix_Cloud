package com.smartlogix.rabbitadmin.service;

import com.smartlogix.rabbitadmin.dto.*;
import com.smartlogix.rabbitadmin.exception.RecursoProtegidoException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsula toda la logica de creacion, consulta, actualizacion y eliminacion
 * de colas, exchanges y bindings en RabbitMQ. Los controladores solo llaman a
 * metodos de alto nivel de este servicio y no conocen detalles internos de la
 * libreria de RabbitMQ (AmqpAdmin).
 *
 * Los recursos que declaran los propios microservicios (ver RecursosSistema)
 * estan protegidos: para modificarlos o eliminarlos hay que pasar force=true.
 */
@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitManagementService management;

    public RabbitAdminService(AmqpAdmin amqpAdmin,
                              RabbitTemplate rabbitTemplate,
                              RabbitManagementService management) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.management = management;
    }

    // ================= COLAS =================

    public void crearCola(CrearColaRequest request) {
        amqpAdmin.declareQueue(construirCola(request.getNombre(), request.isDurable(), request.getDeadLetterExchange()));
    }

    public void eliminarCola(String nombre, boolean force) {
        protegerCola(nombre, force);
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

    /**
     * RabbitMQ no permite cambiar los argumentos de una cola ya declarada, asi que
     * "actualizar" es: leer sus bindings, eliminarla, volver a declararla con los
     * valores nuevos y restaurar los bindings. Los mensajes que tenia se pierden.
     */
    public Map<String, Object> actualizarCola(String nombre, ActualizarColaRequest request, boolean force) {
        protegerCola(nombre, force);
        if (amqpAdmin.getQueueProperties(nombre) == null) {
            throw new IllegalArgumentException("No se encontro la cola: " + nombre);
        }
        // Si la API de management no responde se corta aqui, antes de borrar nada.
        List<Map<String, Object>> bindings = management.bindingsDeCola(nombre);

        amqpAdmin.deleteQueue(nombre);
        amqpAdmin.declareQueue(construirCola(nombre, request.isDurable(), request.getDeadLetterExchange()));
        for (Map<String, Object> b : bindings) {
            amqpAdmin.declareBinding(new Binding(nombre, Binding.DestinationType.QUEUE,
                    String.valueOf(b.get("origen")), String.valueOf(b.get("routingKey")), null));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("status", "actualizada");
        respuesta.put("nombre", nombre);
        respuesta.put("bindingsRestaurados", bindings.size());
        return respuesta;
    }

    public int purgarCola(String nombre) {
        if (amqpAdmin.getQueueProperties(nombre) == null) {
            throw new IllegalArgumentException("No se encontro la cola: " + nombre);
        }
        return amqpAdmin.purgeQueue(nombre);
    }

    public List<Map<String, Object>> verMensajes(String nombre, int cantidad) {
        return management.verMensajes(nombre, cantidad);
    }

    // ================= EXCHANGES =================

    public void crearExchange(CrearExchangeRequest request) {
        amqpAdmin.declareExchange(construirExchange(request.getNombre(), request.getTipo(), request.isDurable()));
    }

    public void eliminarExchange(String nombre, boolean force) {
        protegerExchange(nombre, force);
        boolean eliminado = amqpAdmin.deleteExchange(nombre);
        if (!eliminado) {
            throw new IllegalArgumentException("No se encontro el exchange: " + nombre);
        }
    }

    /**
     * Igual que con las colas: el tipo de un exchange no se puede cambiar en
     * caliente, asi que se eliminan, se recrea y se restauran sus bindings hacia colas.
     */
    public Map<String, Object> actualizarExchange(String nombre, ActualizarExchangeRequest request, boolean force) {
        protegerExchange(nombre, force);
        if (!management.existeExchange(nombre)) {
            throw new IllegalArgumentException("No se encontro el exchange: " + nombre);
        }
        List<Map<String, Object>> bindings = management.bindingsDeExchange(nombre);

        amqpAdmin.deleteExchange(nombre);
        amqpAdmin.declareExchange(construirExchange(nombre, request.getTipo(), request.isDurable()));
        for (Map<String, Object> b : bindings) {
            amqpAdmin.declareBinding(new Binding(String.valueOf(b.get("destino")), Binding.DestinationType.QUEUE,
                    nombre, String.valueOf(b.get("routingKey")), null));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("status", "actualizado");
        respuesta.put("nombre", nombre);
        respuesta.put("bindingsRestaurados", bindings.size());
        return respuesta;
    }

    // ================= BINDINGS =================

    public void crearBinding(CrearBindingRequest request) {
        amqpAdmin.declareBinding(binding(request.getCola(), request.getExchange(), request.getRoutingKey()));
    }

    public void eliminarBinding(CrearBindingRequest request, boolean force) {
        protegerBinding(request.getExchange(), request.getCola(), force);
        amqpAdmin.removeBinding(binding(request.getCola(), request.getExchange(), request.getRoutingKey()));
    }

    public void actualizarBinding(ActualizarBindingRequest request, boolean force) {
        protegerBinding(request.getExchange(), request.getCola(), force);
        amqpAdmin.removeBinding(binding(request.getCola(), request.getExchange(), request.getRoutingKey()));
        amqpAdmin.declareBinding(binding(request.getCola(), request.getExchange(), request.getNuevaRoutingKey()));
    }

    // ================= PUBLICAR (pruebas funcionales) =================

    public void publicar(PublicarMensajeRequest request) {
        MessageProperties props = new MessageProperties();
        props.setContentType(request.getContentType());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        Message mensaje = new Message(request.getMensaje().getBytes(StandardCharsets.UTF_8), props);
        rabbitTemplate.send(request.getExchange(), request.getRoutingKey(), mensaje);
    }

    // ================= INTERNOS =================

    private Queue construirCola(String nombre, boolean durable, String deadLetterExchange) {
        Map<String, Object> args = new HashMap<>();
        if (deadLetterExchange != null && !deadLetterExchange.isBlank()) {
            args.put("x-dead-letter-exchange", deadLetterExchange);
        }
        return new Queue(nombre, durable, false, false, args);
    }

    private Exchange construirExchange(String nombre, String tipo, boolean durable) {
        return switch (tipo) {
            case "direct" -> new DirectExchange(nombre, durable, false);
            case "topic" -> new TopicExchange(nombre, durable, false);
            case "fanout" -> new FanoutExchange(nombre, durable, false);
            case "headers" -> new HeadersExchange(nombre, durable, false);
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + tipo);
        };
    }

    private Binding binding(String cola, String exchange, String routingKey) {
        return new Binding(cola, Binding.DestinationType.QUEUE, exchange,
                routingKey == null ? "" : routingKey, null);
    }

    private void protegerCola(String nombre, boolean force) {
        if (!force && RecursosSistema.colaProtegida(nombre)) {
            throw new RecursoProtegidoException("La cola '" + nombre + "' la declara un microservicio al arrancar "
                    + "y esta protegida. Si de verdad hay que modificarla, repetir con force=true.");
        }
    }

    private void protegerExchange(String nombre, boolean force) {
        if (!force && RecursosSistema.exchangeProtegido(nombre)) {
            throw new RecursoProtegidoException("El exchange '" + nombre + "' lo declara un microservicio al arrancar "
                    + "y esta protegido. Si de verdad hay que modificarlo, repetir con force=true.");
        }
    }

    private void protegerBinding(String exchange, String cola, boolean force) {
        if (!force && RecursosSistema.bindingProtegido(exchange, cola)) {
            throw new RecursoProtegidoException("El binding " + exchange + " -> " + cola + " pertenece a la topologia "
                    + "de un microservicio y esta protegido. Si de verdad hay que modificarlo, repetir con force=true.");
        }
    }
}
