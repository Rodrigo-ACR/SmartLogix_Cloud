package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.config.KafkaConfig;
import com.smartlogix.pedidos.dto.PedidoEventoDTO;
import com.smartlogix.pedidos.model.EstadoPedido;
import com.smartlogix.pedidos.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor Kafka de Pedidos. Publica en smartlogix.pedidos.eventos cada vez
 * que un pedido nace o cambia de estado. Es complementario al evento RabbitMQ
 * "pedido.creado": Rabbit sigue generando el ticket, Kafka alimenta el flujo
 * de streaming (Inventario y Envios reaccionan a estos eventos).
 *
 * La publicacion es "best effort": si Kafka no responde se registra el error
 * pero NO se interrumpe la operacion de negocio (el pedido ya esta guardado).
 */
@Service
public class PedidoKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PedidoKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarPedidoCreado(Pedido pedido) {
        enviar(construir(PedidoEventoDTO.Tipo.PEDIDO_CREADO, pedido, null));
    }

    public void publicarEstadoCambiado(Pedido pedido, EstadoPedido estadoAnterior) {
        enviar(construir(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, pedido, estadoAnterior));
    }

    private PedidoEventoDTO construir(PedidoEventoDTO.Tipo tipo, Pedido p, EstadoPedido anterior) {
        return new PedidoEventoDTO(
                UUID.randomUUID().toString(),
                tipo,
                p.getId(),
                p.getCliente(),
                p.getProductoId(),
                p.getNombreProducto(),
                p.getCantidad(),
                p.getDireccion(),
                p.getGrupoId(),
                anterior != null ? anterior.name() : null,
                p.getEstado() != null ? p.getEstado().name() : null,
                Instant.now(),
                "pedidos");
    }

    private void enviar(PedidoEventoDTO evento) {
        String clave = String.valueOf(evento.pedidoId());
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_PEDIDOS, clave, evento)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("[KAFKA][PEDIDOS] No se pudo publicar {} pedidoId={}: {}",
                                    evento.tipo(), evento.pedidoId(), error.getMessage());
                        } else {
                            log.info(KafkaBanner.recuadro(
                                    "Pedido " + evento.pedidoId(),
                                    "PEDIDOS publico " + evento.tipo() + " (estado: " + evento.estadoNuevo() + ")",
                                    KafkaConfig.TOPIC_PEDIDOS + " | particion " + resultado.getRecordMetadata().partition()
                                            + " | offset " + resultado.getRecordMetadata().offset()));
                        }
                    });
        } catch (Exception e) {
            log.error("[KAFKA][PEDIDOS] Error al publicar {} pedidoId={}: {}",
                    evento.tipo(), evento.pedidoId(), e.getMessage());
        }
    }
}
