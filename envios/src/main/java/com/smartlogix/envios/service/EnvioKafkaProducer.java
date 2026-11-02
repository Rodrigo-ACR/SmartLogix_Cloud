package com.smartlogix.envios.service;

import com.smartlogix.envios.config.KafkaConfig;
import com.smartlogix.envios.dto.LogisticaEventoDTO;
import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.model.EstadoEnvio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka de Envios. Publica en smartlogix.logistica.eventos cuando un
 * envio se crea o cambia de estado. Clave = "ENVIO-id": el topico es compacto
 * (cleanup.policy=compact,delete), Kafka conserva siempre el ultimo estado de
 * cada envio, ideal para que un consumidor nuevo reconstruya el tracking.
 */
@Service
public class EnvioKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(EnvioKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EnvioKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarEnvioCreado(Envio envio) {
        enviar(construir(LogisticaEventoDTO.Tipo.ENVIO_CREADO, envio, null));
    }

    public void publicarEstadoCambiado(Envio envio, EstadoEnvio estadoAnterior) {
        enviar(construir(LogisticaEventoDTO.Tipo.ENVIO_ESTADO_CAMBIADO, envio, estadoAnterior));
    }

    private LogisticaEventoDTO construir(LogisticaEventoDTO.Tipo tipo, Envio e, EstadoEnvio anterior) {
        return new LogisticaEventoDTO(
                UUID.randomUUID().toString(),
                tipo,
                e.getId(),
                e.getPedidoId(),
                e.getEstado() != null ? e.getEstado().name() : null,
                anterior != null ? anterior.name() : null,
                null, null, null, null,
                Instant.now(),
                "envios");
    }

    private void enviar(LogisticaEventoDTO evento) {
        String clave = "ENVIO-" + evento.envioId();
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_LOGISTICA, clave, evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("[KAFKA][ENVIOS] No se pudo publicar {} envioId={}: {}",
                                    evento.tipo(), evento.envioId(), error.getMessage());
                        } else {
                            log.info("[KAFKA][ENVIOS] Publicado {} envioId={} estado={} -> particion={} offset={}",
                                    evento.tipo(), evento.envioId(), evento.estado(),
                                    resultado.getRecordMetadata().partition(),
                                    resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("[KAFKA][ENVIOS] Error al publicar {} envioId={}: {}",
                    evento.tipo(), evento.envioId(), ex.getMessage());
        }
    }
}
