package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Productor de eventos de Pedidos. Se llama desde PedidoService justo
 * despues de guardar un pedido nuevo en la base de datos, para notificar
 * de forma asincrona (sin bloquear la respuesta HTTP) que se genere el
 * ticket/notificacion correspondiente.
 */
@Service
public class PedidoEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoCreado(Long pedidoId, String cliente, Double total) {
        Map<String, Object> evento = Map.of(
                "pedidoId", pedidoId,
                "cliente", cliente,
                "total", total
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PEDIDOS,
                RabbitMQConfig.ROUTING_KEY_CREADO,
                evento
        );
    }
}
