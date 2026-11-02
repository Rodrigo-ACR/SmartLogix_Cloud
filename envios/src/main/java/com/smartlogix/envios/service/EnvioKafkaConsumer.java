package com.smartlogix.envios.service;

import com.smartlogix.envios.config.KafkaConfig;
import com.smartlogix.envios.dto.PedidoEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor Kafka de Envios: escucha smartlogix.pedidos.eventos con su propio
 * consumer group (envios-pedidos-grp), independiente del de Inventario: los dos
 * reciben TODOS los eventos de pedidos, cada uno en su ritmo y con su propio offset.
 */
@Service
public class EnvioKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(EnvioKafkaConsumer.class);

    private final EnvioAutomaticoService envioAutomaticoService;

    public EnvioKafkaConsumer(EnvioAutomaticoService envioAutomaticoService) {
        this.envioAutomaticoService = envioAutomaticoService;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_PEDIDOS,
            groupId = KafkaConfig.GROUP_PEDIDOS,
            containerFactory = "pedidosListenerFactory")
    public void onEventoPedido(ConsumerRecord<String, PedidoEventoDTO> record) {
        PedidoEventoDTO evento = record.value();
        validar(evento);
        log.info(KafkaBanner.recuadro(
                "Pedido " + evento.pedidoId(),
                "ENVIOS recibio " + evento.tipo(),
                "cliente: " + evento.cliente() + " | estado: " + evento.estadoAnterior() + " -> " + evento.estadoNuevo(),
                KafkaConfig.TOPIC_PEDIDOS + " | particion " + record.partition() + " | offset " + record.offset()));
        envioAutomaticoService.procesarPedido(evento);
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
