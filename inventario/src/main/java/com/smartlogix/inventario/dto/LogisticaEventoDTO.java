package com.smartlogix.inventario.dto;

import java.time.Instant;

/**
 * Evento logistico: cambios en envios y alertas de inventario. Viaja por el
 * topico smartlogix.logistica.eventos (clave = "ENVIO-id" o "PRODUCTO-id"; el
 * topico usa cleanup.policy=compact,delete, o sea que Kafka conserva siempre el
 * ultimo evento de cada envio / producto).
 *
 * Campos:
 *  - eventId        identificador unico del evento
 *  - tipo           ENVIO_CREADO, ENVIO_ESTADO_CAMBIADO o STOCK_BAJO
 *  - envioId, pedidoId, estado, estadoAnterior   (eventos de envio)
 *  - productoId, nombreProducto, stockActual, umbral   (alerta de stock)
 *  - ocurridoEn     instante en que ocurrio (UTC, ISO-8601)
 *  - origen         microservicio que publico el evento
 */
public record LogisticaEventoDTO(
        String eventId,
        LogisticaEventoDTO.Tipo tipo,
        Long envioId,
        Long pedidoId,
        String estado,
        String estadoAnterior,
        Long productoId,
        String nombreProducto,
        Integer stockActual,
        Integer umbral,
        Instant ocurridoEn,
        String origen) {

    public enum Tipo {
        ENVIO_CREADO,
        ENVIO_ESTADO_CAMBIADO,
        STOCK_BAJO
    }
}
