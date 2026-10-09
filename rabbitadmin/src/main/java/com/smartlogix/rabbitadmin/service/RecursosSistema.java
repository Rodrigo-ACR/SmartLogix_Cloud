package com.smartlogix.rabbitadmin.service;

import java.util.Set;

/**
 * Recursos que declaran los propios microservicios en su RabbitMQConfig.
 * Si se eliminan o se recrean, los @RabbitListener de ese microservicio
 * dejan de funcionar, por eso rabbitadmin los protege (se pueden tocar solo
 * con force=true).
 */
public final class RecursosSistema {

    public static final Set<String> COLAS = Set.of(
            "cola.pedidos.creado", "cola.pedidos.dlq",
            "cola.envios.notificacion", "cola.envios.dlq",
            "cola.usuarios.bienvenida", "cola.usuarios.dlq",
            "cola.inventario.stockbajo", "cola.inventario.dlq"
    );

    public static final Set<String> EXCHANGES = Set.of(
            "pedidos.direct.exchange", "pedidos.dlx",
            "notificaciones.topic.exchange", "envios.dlx", "usuarios.dlx",
            "inventario.fanout.exchange", "inventario.dlx"
    );

    private RecursosSistema() {}

    public static boolean colaProtegida(String nombre) {
        return COLAS.contains(nombre);
    }

    public static boolean exchangeProtegido(String nombre) {
        return EXCHANGES.contains(nombre);
    }

    public static boolean bindingProtegido(String exchange, String cola) {
        return exchangeProtegido(exchange) || colaProtegida(cola);
    }
}
