package com.smartlogix.usuarios.config;

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
 * Configuracion centralizada de RabbitMQ para el microservicio de Usuarios.
 *
 * Comparte el mismo Exchange TOPIC que el microservicio de Envios
 * (notificaciones.topic.exchange). Cada microservicio declara el exchange
 * de forma independiente (Spring AMQP lo hace idempotente: si ya existe
 * en el broker con la misma configuracion, no se duplica), pero cada uno
 * declara y consume UNICAMENTE su propia cola.
 *
 * Routing key usada por Usuarios: "usuarios.bienvenida"
 */
@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "notificaciones.topic.exchange";

    public static final String QUEUE_USUARIO_BIENVENIDA = "cola.usuarios.bienvenida";
    public static final String ROUTING_KEY_BIENVENIDA = "usuarios.bienvenida";

    public static final String DLX_USUARIOS = "usuarios.dlx";
    public static final String DLQ_USUARIOS = "cola.usuarios.dlq";

    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue colaUsuarioBienvenida() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_USUARIOS);
        return new Queue(QUEUE_USUARIO_BIENVENIDA, true, false, false, args);
    }

    @Bean
    public Binding bindingUsuarioBienvenida(Queue colaUsuarioBienvenida, TopicExchange notificacionesExchange) {
        return BindingBuilder.bind(colaUsuarioBienvenida).to(notificacionesExchange).with(ROUTING_KEY_BIENVENIDA);
    }

    @Bean
    public FanoutExchange usuariosDlxExchange() {
        return new FanoutExchange(DLX_USUARIOS);
    }

    @Bean
    public Queue usuariosDlq() {
        return new Queue(DLQ_USUARIOS, true);
    }

    @Bean
    public Binding bindingUsuariosDlq(Queue usuariosDlq, FanoutExchange usuariosDlxExchange) {
        return BindingBuilder.bind(usuariosDlq).to(usuariosDlxExchange);
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
