package com.smartlogix.envios.service;

import com.rabbitmq.client.Channel;
import com.smartlogix.envios.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class EnvioEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ENVIO_ACTUALIZADO, containerFactory = "manualAckFactory")
    public void procesarEnvioActualizado(@Payload Map<String, Object> evento,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Object envioId = evento.get("envioId");
            Object estado = evento.get("estado");

            if (envioId == null || estado == null) {
                System.err.println("ENVIOS: evento invalido, enviando a DLQ -> " + evento);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            System.out.println("ENVIOS: notificacion enviada para envio " + envioId
                    + " con nuevo estado " + estado);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            System.err.println("ENVIOS: error procesando evento, enviando a DLQ -> " + e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
