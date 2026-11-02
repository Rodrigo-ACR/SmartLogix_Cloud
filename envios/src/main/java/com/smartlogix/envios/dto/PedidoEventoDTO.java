package com.smartlogix.envios.dto;

import java.time.Instant;

/**
 * Evento del ciclo de vida de un pedido. Viaja por el topico
 * smartlogix.pedidos.eventos (clave = pedidoId, asi todos los eventos de un
 * mismo pedido caen en la misma particion y se procesan en orden).
 *
 * Campos:
 *  - eventId        identificador unico del evento (permite detectar duplicados)
 *  - tipo           que paso: PEDIDO_CREADO o PEDIDO_ESTADO_CAMBIADO
 *  - pedidoId       pedido afectado
 *  - cliente, productoId, nombreProducto, cantidad   que se pidio y quien
 *  - direccion, grupoId   datos que necesita Envios para crear el despacho
 *  - estadoAnterior / estadoNuevo   transicion (solo en PEDIDO_ESTADO_CAMBIADO)
 *  - ocurridoEn     instante en que ocurrio (UTC, ISO-8601)
 *  - origen         microservicio que publico el evento
 */
public record PedidoEventoDTO(
        String eventId,
        PedidoEventoDTO.Tipo tipo,
        Long pedidoId,
        String cliente,
        Long productoId,
        String nombreProducto,
        Integer cantidad,
        String direccion,
        String grupoId,
        String estadoAnterior,
        String estadoNuevo,
        Instant ocurridoEn,
        String origen) {

    public enum Tipo {
        PEDIDO_CREADO,
        PEDIDO_ESTADO_CAMBIADO
    }
}
