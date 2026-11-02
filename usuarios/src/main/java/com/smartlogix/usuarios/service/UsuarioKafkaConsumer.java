package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.config.KafkaConfig;
import com.smartlogix.usuarios.dto.LogisticaEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumidor Kafka de Usuarios: escucha smartlogix.logistica.eventos con su
 * propio consumer group (usuarios-alertas-grp). Pedidos escucha el mismo topico
 * con otro grupo (pedidos-logistica-grp), asi que ambos reciben cada evento.
 */
@Service
public class UsuarioKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UsuarioKafkaConsumer.class);

    private final NotificacionAdminService notificacionAdminService;

    public UsuarioKafkaConsumer(NotificacionAdminService notificacionAdminService) {
        this.notificacionAdminService = notificacionAdminService;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_LOGISTICA,
            groupId = KafkaConfig.GROUP_ALERTAS,
            containerFactory = "logisticaListenerFactory")
    public void onEventoLogistico(ConsumerRecord<String, LogisticaEventoDTO> record) {
        LogisticaEventoDTO evento = record.value();
        validar(evento);
        log.info("[KAFKA][USUARIOS] <- {} key={} particion={} offset={}",
                evento.tipo(), record.key(), record.partition(), record.offset());
        notificacionAdminService.procesar(evento);
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
        if (e.tipo() == LogisticaEventoDTO.Tipo.STOCK_BAJO && e.productoId() == null) {
            throw new IllegalArgumentException("Evento de stock invalido: productoId obligatorio");
        }
    }
}
