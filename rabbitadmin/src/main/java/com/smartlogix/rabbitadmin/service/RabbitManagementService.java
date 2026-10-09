package com.smartlogix.rabbitadmin.service;

import com.smartlogix.rabbitadmin.exception.RabbitNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.function.Function;

/**
 * Lectura de la topologia real del broker usando la API HTTP del plugin de
 * management de RabbitMQ. AmqpAdmin solo sabe crear, borrar y consultar un
 * recurso por nombre; no puede listar. Esta clase cubre esa parte para poder
 * mostrar en el frontend todas las colas, exchanges y bindings.
 *
 * Se prueban en orden las URLs configuradas (rabbitmq1 y rabbitmq2), asi la
 * vista sigue funcionando si un nodo del cluster no responde.
 */
@Service
public class RabbitManagementService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};

    private static final String VHOST = "/";
    private static final String NOMBRE_VALIDO = "^[a-zA-Z0-9_.\\-]+$";

    private final RestTemplate rest;
    private final List<String> bases = new ArrayList<>();

    public RabbitManagementService(
            @Value("${rabbitmq.management.urls}") String urls,
            @Value("${spring.rabbitmq.username}") String usuario,
            @Value("${spring.rabbitmq.password}") String password) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.rest = new RestTemplate(factory);
        this.rest.getInterceptors().add(new BasicAuthenticationInterceptor(usuario, password));

        for (String u : urls.split(",")) {
            String limpia = u.trim();
            if (!limpia.isEmpty()) {
                bases.add(limpia.endsWith("/") ? limpia.substring(0, limpia.length() - 1) : limpia);
            }
        }
    }

    // ================= VISTA GENERAL =================

    public Map<String, Object> overview() {
        return conFallback(base -> {
            Map<String, Object> salida = new LinkedHashMap<>();
            salida.put("origen", base);
            salida.put("nodos", resumirNodos(get(base, "/api/nodes")));
            salida.put("colas", resumirColas(get(base, "/api/queues")));
            salida.put("exchanges", resumirExchanges(get(base, "/api/exchanges")));
            salida.put("bindings", resumirBindings(get(base, "/api/bindings")));
            return salida;
        });
    }

    // ================= CONSULTAS PUNTUALES =================

    /** Bindings (exchange -> cola) de una cola. Se usa para restaurarlos tras recrearla. */
    public List<Map<String, Object>> bindingsDeCola(String cola) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> b : resumirBindings(conFallback(base -> get(base, "/api/bindings")))) {
            if (cola.equals(b.get("destino")) && "queue".equals(b.get("tipoDestino"))) {
                resultado.add(b);
            }
        }
        return resultado;
    }

    /** Bindings (exchange -> cola) que salen de un exchange. Se usa para restaurarlos tras recrearlo. */
    public List<Map<String, Object>> bindingsDeExchange(String exchange) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> b : resumirBindings(conFallback(base -> get(base, "/api/bindings")))) {
            if (exchange.equals(b.get("origen")) && "queue".equals(b.get("tipoDestino"))) {
                resultado.add(b);
            }
        }
        return resultado;
    }

    public boolean existeExchange(String nombre) {
        for (Map<String, Object> e : resumirExchanges(conFallback(base -> get(base, "/api/exchanges")))) {
            if (nombre.equals(e.get("nombre"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mira los primeros mensajes de una cola sin consumirlos (ack_requeue_true:
     * se devuelven a la cola, solo queda marcado "redelivered"). Sirve para
     * mostrar el contenido y la cabecera x-death de los mensajes de una DLQ.
     */
    public List<Map<String, Object>> verMensajes(String cola, int cantidad) {
        if (!cola.matches(NOMBRE_VALIDO)) {
            throw new IllegalArgumentException("Nombre de cola invalido: " + cola);
        }
        int n = Math.max(1, Math.min(cantidad, 20));
        return conFallback(base -> {
            URI uri = URI.create(base + "/api/queues/%2F/" + cola + "/get");
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("count", n);
            cuerpo.put("ackmode", "ack_requeue_true");
            cuerpo.put("encoding", "auto");
            cuerpo.put("truncate", 20000);
            try {
                List<Map<String, Object>> r = rest.exchange(uri, HttpMethod.POST,
                        new HttpEntity<>(cuerpo), LISTA).getBody();
                return r == null ? new ArrayList<Map<String, Object>>() : r;
            } catch (HttpClientErrorException.NotFound e) {
                throw new IllegalArgumentException("No se encontro la cola: " + cola);
            }
        });
    }

    // ================= INTERNOS =================

    private List<Map<String, Object>> get(String base, String ruta) {
        List<Map<String, Object>> r = rest.exchange(URI.create(base + ruta), HttpMethod.GET, null, LISTA).getBody();
        return r == null ? new ArrayList<>() : r;
    }

    private <T> T conFallback(Function<String, T> operacion) {
        RestClientException ultimo = null;
        for (String base : bases) {
            try {
                return operacion.apply(base);
            } catch (RestClientException e) {
                ultimo = e;
            }
        }
        throw new RabbitNoDisponibleException("No se pudo consultar la API de management de RabbitMQ"
                + (ultimo != null ? ": " + ultimo.getMessage() : " (sin URLs configuradas)"));
    }

    private List<Map<String, Object>> resumirNodos(List<Map<String, Object>> nodos) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> n : nodos) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", n.get("name"));
            m.put("activo", Boolean.TRUE.equals(n.get("running")));
            m.put("tipo", n.get("type"));
            m.put("uptimeMs", n.get("uptime"));
            salida.add(m);
        }
        salida.sort(Comparator.comparing(m -> String.valueOf(m.get("nombre"))));
        return salida;
    }

    private List<Map<String, Object>> resumirColas(List<Map<String, Object>> colas) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> q : colas) {
            if (!VHOST.equals(q.get("vhost"))) continue;
            String nombre = String.valueOf(q.get("name"));
            Map<String, Object> args = q.get("arguments") instanceof Map<?, ?> a
                    ? castMapa(a) : new HashMap<>();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", nombre);
            m.put("durable", Boolean.TRUE.equals(q.get("durable")));
            m.put("estado", q.get("state"));
            m.put("nodo", q.get("node"));
            m.put("listos", numero(q.get("messages_ready")));
            m.put("sinAck", numero(q.get("messages_unacknowledged")));
            m.put("total", numero(q.get("messages")));
            m.put("consumidores", numero(q.get("consumers")));
            m.put("dlx", args.get("x-dead-letter-exchange"));
            m.put("protegido", RecursosSistema.colaProtegida(nombre));
            salida.add(m);
        }
        salida.sort(Comparator.comparing(m -> String.valueOf(m.get("nombre"))));
        return salida;
    }

    private List<Map<String, Object>> resumirExchanges(List<Map<String, Object>> exchanges) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> e : exchanges) {
            if (!VHOST.equals(e.get("vhost"))) continue;
            String nombre = String.valueOf(e.get("name"));
            if (nombre.isEmpty() || "null".equals(nombre)) continue; // exchange por defecto
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", nombre);
            m.put("tipo", e.get("type"));
            m.put("durable", Boolean.TRUE.equals(e.get("durable")));
            m.put("interno", nombre.startsWith("amq."));
            m.put("protegido", RecursosSistema.exchangeProtegido(nombre));
            salida.add(m);
        }
        salida.sort(Comparator.comparing(m -> String.valueOf(m.get("nombre"))));
        return salida;
    }

    private List<Map<String, Object>> resumirBindings(List<Map<String, Object>> bindings) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> b : bindings) {
            if (!VHOST.equals(b.get("vhost"))) continue;
            String origen = String.valueOf(b.get("source"));
            if (origen.isEmpty() || "null".equals(origen)) continue; // bindings implicitos del exchange por defecto
            String destino = String.valueOf(b.get("destination"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("origen", origen);
            m.put("destino", destino);
            m.put("tipoDestino", b.get("destination_type"));
            m.put("routingKey", b.get("routing_key") == null ? "" : b.get("routing_key"));
            m.put("protegido", RecursosSistema.bindingProtegido(origen, destino));
            salida.add(m);
        }
        salida.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("origen")))
                .thenComparing(m -> String.valueOf(m.get("destino"))));
        return salida;
    }

    private long numero(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMapa(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }
}
