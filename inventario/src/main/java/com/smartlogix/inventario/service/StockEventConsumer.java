package com.smartlogix.inventario.service;

import com.rabbitmq.client.Channel;
import com.smartlogix.inventario.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class StockEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_BAJO, containerFactory = "manualAckFactory")
    public void procesarStockBajo(@Payload Map<String, Object> evento,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Object productoId = evento.get("productoId");
            Object nombreProducto = evento.get("nombreProducto");

            if (productoId == null || nombreProducto == null) {
                System.err.println("INVENTARIO: evento invalido, enviando a DLQ -> " + evento);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            System.out.println("INVENTARIO: alerta de stock bajo generada para producto "
                    + nombreProducto + " (id=" + productoId + "), stock actual: " + evento.get("stockActual"));

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            System.err.println("INVENTARIO: error procesando evento, enviando a DLQ -> " + e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
