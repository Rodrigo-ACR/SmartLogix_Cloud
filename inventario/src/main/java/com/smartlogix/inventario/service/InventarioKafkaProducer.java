package com.smartlogix.inventario.service;

import com.smartlogix.inventario.config.KafkaConfig;
import com.smartlogix.inventario.dto.LogisticaEventoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka de Inventario. Publica STOCK_BAJO en
 * smartlogix.logistica.eventos cuando un producto cae bajo el umbral minimo.
 * Complementa a la alerta fanout de RabbitMQ (que sigue funcionando igual).
 * Clave = "PRODUCTO-id": el topico es compacto, Kafka conserva solo la ultima
 * alerta de cada producto.
 */
@Service
public class InventarioKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(InventarioKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventarioKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarStockBajo(Long productoId, String nombreProducto, int stockActual, int umbral) {
        LogisticaEventoDTO evento = new LogisticaEventoDTO(
                UUID.randomUUID().toString(),
                LogisticaEventoDTO.Tipo.STOCK_BAJO,
                null, null, null, null,
                productoId, nombreProducto, stockActual, umbral,
                Instant.now(),
                "inventario");
        String clave = "PRODUCTO-" + productoId;
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_LOGISTICA, clave, evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("[KAFKA][INVENTARIO] No se pudo publicar STOCK_BAJO productoId={}: {}",
                                    productoId, error.getMessage());
                        } else {
                            log.info(KafkaBanner.recuadro(
                                    "Producto " + productoId + " (" + nombreProducto + ")",
                                    "INVENTARIO publico STOCK_BAJO | stock: " + stockActual + " | umbral: " + umbral,
                                    KafkaConfig.TOPIC_LOGISTICA + " | particion " + resultado.getRecordMetadata().partition()
                                            + " | offset " + resultado.getRecordMetadata().offset()));
                        }
                    });
        } catch (Exception e) {
            log.error("[KAFKA][INVENTARIO] Error al publicar STOCK_BAJO productoId={}: {}", productoId, e.getMessage());
        }
    }
}
