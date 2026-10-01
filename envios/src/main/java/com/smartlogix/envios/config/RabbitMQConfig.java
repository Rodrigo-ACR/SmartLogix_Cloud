package com.smartlogix.envios.config;

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
 * Configuracion centralizada de RabbitMQ para el microservicio de Envios.
 *
 * Este microservicio usa un Exchange de tipo TOPIC, compartido logicamente
 * con el microservicio de Usuarios (ambos declaran el mismo exchange por
 * nombre; RabbitMQ lo trata como una unica entidad en el broker). Cada
 * microservicio declara y consume SOLO su propia cola.
 *
 * Routing key usada por Envios: "envios.actualizado"
 */
@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "notificaciones.topic.exchange";

    public static final String QUEUE_ENVIO_ACTUALIZADO = "cola.envios.notificacion";
    public static final String ROUTING_KEY_ENVIO = "envios.actualizado";

    // Dead Letter especifica de esta cola
    public static final String DLX_ENVIOS = "envios.dlx";
    public static final String DLQ_ENVIOS = "cola.envios.dlq";

    @Bean
    public TopicExchange notificacionesExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue colaEnvioActualizado() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_ENVIOS);
        return new Queue(QUEUE_ENVIO_ACTUALIZADO, true, false, false, args);
    }

    @Bean
    public Binding bindingEnvioActualizado(Queue colaEnvioActualizado, TopicExchange notificacionesExchange) {
        return BindingBuilder.bind(colaEnvioActualizado).to(notificacionesExchange).with(ROUTING_KEY_ENVIO);
    }

    @Bean
    public FanoutExchange enviosDlxExchange() {
        return new FanoutExchange(DLX_ENVIOS);
    }

    @Bean
    public Queue enviosDlq() {
        return new Queue(DLQ_ENVIOS, true);
    }

    @Bean
    public Binding bindingEnviosDlq(Queue enviosDlq, FanoutExchange enviosDlxExchange) {
        return BindingBuilder.bind(enviosDlq).to(enviosDlxExchange);
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
