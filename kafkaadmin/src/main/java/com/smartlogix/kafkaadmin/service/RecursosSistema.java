package com.smartlogix.kafkaadmin.service;

import java.util.Set;

/**
 * Topicos que usan los microservicios (Pedidos, Inventario, Envios, Usuarios).
 * Si se eliminan, los consumidores y productores dejan de funcionar, por eso
 * kafkaadmin pide confirmar con force=true (mismo criterio que rabbitadmin).
 */
public final class RecursosSistema {

    public static final Set<String> TOPICOS = Set.of(
            "smartlogix.pedidos.eventos",
            "smartlogix.logistica.eventos",
            "smartlogix.pedidos.eventos.DLT",
            "smartlogix.logistica.eventos.DLT"
    );

    private RecursosSistema() {}

    public static boolean topicoProtegido(String nombre) {
        return TOPICOS.contains(nombre);
    }
}
