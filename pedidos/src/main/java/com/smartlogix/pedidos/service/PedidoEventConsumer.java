package com.smartlogix.pedidos.service;

import com.rabbitmq.client.Channel;
import com.smartlogix.pedidos.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Consumidor del evento "pedido.creado". Procesa la generacion de la
 * notificacion/ticket de forma asincrona. Maneja confirmacion (ACK)
 * explicita: si el procesamiento falla, rechaza el mensaje (NACK sin
 * reintentar) para que RabbitMQ lo redirija automaticamente a la Dead
 * Letter Queue de este microservicio (cola.pedidos.dlq).
 */
@Service
public class PedidoEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PEDIDO_CREADO, containerFactory = "manualAckFactory")
    public void procesarPedidoCreado(@Payload Map<String, Object> evento,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Object pedidoId = evento.get("pedidoId");
            Object cliente = evento.get("cliente");

            if (pedidoId == null || cliente == null) {
                // Mensaje invalido/incompleto: se rechaza sin reintentar -> va a la DLQ
                System.err.println("PEDIDOS: evento invalido, enviando a DLQ -> " + evento);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            // Aqui iria la logica real: generar ticket, enviar notificacion, etc.
            System.out.println("PEDIDOS: ticket/notificacion generado para pedido " + pedidoId
                    + " del cliente " + cliente);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            System.err.println("PEDIDOS: error procesando evento, enviando a DLQ -> " + e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
