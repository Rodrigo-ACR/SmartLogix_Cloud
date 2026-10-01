package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UsuarioEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public UsuarioEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarUsuarioNuevo(String nombre, String correo) {
        Map<String, Object> evento = Map.of(
                "nombre", nombre,
                "correo", correo
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TOPIC_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_BIENVENIDA,
                evento
        );
    }
}
