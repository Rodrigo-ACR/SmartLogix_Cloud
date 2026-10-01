package com.smartlogix.envios.service;

import com.smartlogix.envios.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EnvioEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public EnvioEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarEnvioActualizado(Long envioId, String estado) {
        Map<String, Object> evento = Map.of(
                "envioId", envioId,
                "estado", estado
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TOPIC_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ENVIO,
                evento
        );
    }
}
