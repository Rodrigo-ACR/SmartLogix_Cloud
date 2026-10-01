package com.smartlogix.pedidos.config;

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
 * Configuracion centralizada de RabbitMQ para el microservicio de Pedidos.
 * Todos los nombres de exchange, cola y routing key se definen aqui como
 * constantes, para que ningun otro archivo del microservicio tenga
 * strings sueltos con estos valores (EP2 - indicador de centralizacion).
 *
 * Este microservicio usa un Exchange de tipo DIRECT: cuando se crea un
 * pedido, se publica un evento "pedido.creado" que es consumido de forma
 * asincrona para generar una notificacion/ticket, sin bloquear la
 * respuesta HTTP de creacion del pedido.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_PEDIDOS   = "pedidos.direct.exchange";
    public static final String QUEUE_PEDIDO_CREADO = "cola.pedidos.creado";
    public static final String ROUTING_KEY_CREADO  = "pedido.creado";

    // Dead Letter especifica de ESTA cola (cada cola de la EP2 tiene su propia DLQ)
    public static final String DLX_PEDIDOS = "pedidos.dlx";
    public static final String DLQ_PEDIDOS = "cola.pedidos.dlq";

    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE_PEDIDOS, true, false);
    }

    @Bean
    public Queue colaPedidoCreado() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_PEDIDOS);
        return new Queue(QUEUE_PEDIDO_CREADO, true, false, false, args);
    }

    @Bean
    public Binding bindingPedidoCreado(Queue colaPedidoCreado, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(colaPedidoCreado).to(pedidosExchange).with(ROUTING_KEY_CREADO);
    }

    // Dead Letter Exchange y Queue propios del microservicio de Pedidos
    @Bean
    public FanoutExchange pedidosDlxExchange() {
        return new FanoutExchange(DLX_PEDIDOS);
    }

    @Bean
    public Queue pedidosDlq() {
        return new Queue(DLQ_PEDIDOS, true);
    }

    @Bean
    public Binding bindingPedidosDlq(Queue pedidosDlq, FanoutExchange pedidosDlxExchange) {
        return BindingBuilder.bind(pedidosDlq).to(pedidosDlxExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Fabrica de listeners con confirmacion (ACK) manual: permite que el
    // consumidor decida explicitamente si confirma o rechaza cada mensaje.
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
