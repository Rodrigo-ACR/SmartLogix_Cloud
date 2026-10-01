package com.smartlogix.inventario.service;

import com.smartlogix.inventario.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StockEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public StockEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Fanout: no se especifica routing key (se envia "")
    public void publicarStockBajo(Long productoId, String nombreProducto, Integer stockActual) {
        Map<String, Object> evento = Map.of(
                "productoId", productoId,
                "nombreProducto", nombreProducto,
                "stockActual", stockActual
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.FANOUT_EXCHANGE, "", evento);
    }
}
