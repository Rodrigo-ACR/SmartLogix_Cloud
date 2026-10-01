package com.smartlogix.inventario.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion centralizada de RabbitMQ para el microservicio de
 * Inventario.
 *
 * Este microservicio usa un Exchange de tipo FANOUT: cuando el stock de
 * un producto cae bajo el umbral minimo, se publica una alerta que se
 * difunde a todos los suscriptores sin distincion (no se usa routing
 * key). Junto con "pedidos" (direct) y "envios/usuarios" (topic), esto
 * completa los tres tipos de exchange mas comunes en el sistema.
 */
@Configuration
public class RabbitMQConfig {

    public static final String FANOUT_EXCHANGE = "inventario.fanout.exchange";
    public static final String QUEUE_STOCK_BAJO = "cola.inventario.stockbajo";

    public static final String DLX_INVENTARIO = "inventario.dlx";
    public static final String DLQ_INVENTARIO = "cola.inventario.dlq";

    @Bean
    public FanoutExchange inventarioExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue colaStockBajo() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_INVENTARIO);
        return new Queue(QUEUE_STOCK_BAJO, true, false, false, args);
    }

    // En un exchange FANOUT no se usa routing key: el binding simplemente
    // conecta la cola al exchange y recibe copia de todo lo publicado.
    @Bean
    public Binding bindingStockBajo(Queue colaStockBajo, FanoutExchange inventarioExchange) {
        return BindingBuilder.bind(colaStockBajo).to(inventarioExchange);
    }

    @Bean
    public FanoutExchange inventarioDlxExchange() {
        return new FanoutExchange(DLX_INVENTARIO);
    }

    @Bean
    public Queue inventarioDlq() {
        return new Queue(DLQ_INVENTARIO, true);
    }

    @Bean
    public Binding bindingInventarioDlq(Queue inventarioDlq, FanoutExchange inventarioDlxExchange) {
        return BindingBuilder.bind(inventarioDlq).to(inventarioDlxExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory manualAckFactory(ConnectionFactory connectionFactory,
                                                                    MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
