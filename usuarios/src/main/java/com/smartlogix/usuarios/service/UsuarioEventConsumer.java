package com.smartlogix.usuarios.service;

import com.rabbitmq.client.Channel;
import com.smartlogix.usuarios.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class UsuarioEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USUARIO_BIENVENIDA, containerFactory = "manualAckFactory")
    public void procesarUsuarioNuevo(@Payload Map<String, Object> evento,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Object nombre = evento.get("nombre");
            Object correo = evento.get("correo");

            if (nombre == null || correo == null) {
                System.err.println("USUARIOS: evento invalido, enviando a DLQ -> " + evento);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            System.out.println("USUARIOS: notificacion de bienvenida generada para " + nombre + " (" + correo + ")");

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            System.err.println("USUARIOS: error procesando evento, enviando a DLQ -> " + e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
