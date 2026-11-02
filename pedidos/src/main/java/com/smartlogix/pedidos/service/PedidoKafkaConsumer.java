package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.config.KafkaConfig;
import com.smartlogix.pedidos.dto.LogisticaEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor Kafka de Pedidos: escucha smartlogix.logistica.eventos.
 * Solo valida el mensaje y delega; la logica vive en SeguimientoLogisticoService.
 *
 * Si el payload es invalido se lanza IllegalArgumentException: el
 * DefaultErrorHandler lo manda directo al DLT (smartlogix.logistica.eventos.DLT)
 * sin reintentar, y el resto de la particion sigue fluyendo.
 */
@Service
public class PedidoKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaConsumer.class);

    private final SeguimientoLogisticoService seguimiento;

    public PedidoKafkaConsumer(SeguimientoLogisticoService seguimiento) {
        this.seguimiento = seguimiento;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_LOGISTICA,
            groupId = KafkaConfig.GROUP_LOGISTICA,
            containerFactory = "logisticaListenerFactory")
    public void onEventoLogistico(ConsumerRecord<String, LogisticaEventoDTO> record) {
        LogisticaEventoDTO evento = record.value();
        validar(evento);
        log.info(KafkaBanner.recuadro(
                evento.pedidoId() != null ? "Pedido " + evento.pedidoId() : "Producto " + evento.productoId(),
                "PEDIDOS recibio " + evento.tipo() + (evento.estado() != null ? " (estado: " + evento.estado() + ")" : ""),
                evento.envioId() != null ? "envio: " + evento.envioId() : null,
                KafkaConfig.TOPIC_LOGISTICA + " | particion " + record.partition() + " | offset " + record.offset()));
        seguimiento.registrar(evento);
    }

    private void validar(LogisticaEventoDTO e) {
        if (e == null) {
            throw new IllegalArgumentException("Evento nulo (payload vacio)");
        }
        if (e.eventId() == null || e.eventId().isBlank()) {
            throw new IllegalArgumentException("Evento invalido: eventId obligatorio");
        }
        if (e.tipo() == null) {
            throw new IllegalArgumentException("Evento invalido: tipo desconocido o ausente");
        }
        boolean esEnvio = e.tipo() == LogisticaEventoDTO.Tipo.ENVIO_CREADO
                || e.tipo() == LogisticaEventoDTO.Tipo.ENVIO_ESTADO_CAMBIADO;
        if (esEnvio && (e.envioId() == null || e.pedidoId() == null)) {
            throw new IllegalArgumentException("Evento de envio invalido: envioId y pedidoId obligatorios");
        }
    }
}
