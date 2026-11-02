package com.smartlogix.inventario.service;

import com.smartlogix.inventario.config.KafkaConfig;
import com.smartlogix.inventario.dto.PedidoEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor Kafka de Inventario: escucha smartlogix.pedidos.eventos.
 * Valida el mensaje y delega en ReposicionStockService (la logica de negocio
 * no depende de Kafka).
 */
@Service
public class InventarioKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventarioKafkaConsumer.class);

    private final ReposicionStockService reposicionStockService;

    public InventarioKafkaConsumer(ReposicionStockService reposicionStockService) {
        this.reposicionStockService = reposicionStockService;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_PEDIDOS,
            groupId = KafkaConfig.GROUP_PEDIDOS,
            containerFactory = "pedidosListenerFactory")
    public void onEventoPedido(ConsumerRecord<String, PedidoEventoDTO> record) {
        PedidoEventoDTO evento = record.value();
        validar(evento);
        log.info(KafkaBanner.recuadro(
                "Pedido " + evento.pedidoId(),
                "INVENTARIO recibio " + evento.tipo(),
                "cliente: " + evento.cliente() + " | estado: " + evento.estadoAnterior() + " -> " + evento.estadoNuevo(),
                KafkaConfig.TOPIC_PEDIDOS + " | particion " + record.partition() + " | offset " + record.offset()));
        reposicionStockService.procesar(evento);
    }

    private void validar(PedidoEventoDTO e) {
        if (e == null) {
            throw new IllegalArgumentException("Evento nulo (payload vacio)");
        }
        if (e.eventId() == null || e.eventId().isBlank()) {
            throw new IllegalArgumentException("Evento invalido: eventId obligatorio");
        }
        if (e.tipo() == null) {
            throw new IllegalArgumentException("Evento invalido: tipo desconocido o ausente");
        }
        if (e.pedidoId() == null) {
            throw new IllegalArgumentException("Evento invalido: pedidoId obligatorio");
        }
    }
}
