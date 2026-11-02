package com.smartlogix.usuarios.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogix.usuarios.dto.LogisticaEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuracion centralizada del cliente Kafka del microservicio Usuarios (EV3).
 *
 * Reune en un solo lugar: conexion a los brokers, serializacion JSON de los
 * DTO, consumer group, estrategia de reintentos y Dead Letter Topic (DLT).
 * Ningun otro archivo del microservicio tiene nombres de topicos ni grupos
 * escritos a mano.
 *
 * Topicos del sistema (creados con 3 particiones y 3 replicas):
 *   - smartlogix.pedidos.eventos    -> ciclo de vida de los pedidos
 *   - smartlogix.logistica.eventos  -> envios y alertas de stock
 *   - &lt;topico&gt;.DLT                  -> mensajes que agotaron los reintentos
 *
 * Los valores de conexion vienen del application.properties / variables de
 * entorno (KAFKA_BOOTSTRAP_SERVERS), asi el mismo codigo corre en local y en AWS.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    // ---- Topicos compartidos por todo el sistema ----
    public static final String TOPIC_PEDIDOS = "smartlogix.pedidos.eventos";
    public static final String TOPIC_LOGISTICA = "smartlogix.logistica.eventos";
    public static final String DLT_SUFFIX = ".DLT";

    // ---- Consumer group de este microservicio ----
    public static final String GROUP_ALERTAS = "usuarios-alertas-grp";

    @Value("${spring.kafka.bootstrap-servers:localhost:19092}")
    private String bootstrapServers;

    @Value("${smartlogix.kafka.producer.max-block-ms:10000}")
    private int producerMaxBlockMs;

    @Value("${smartlogix.kafka.listener.concurrency:3}")
    private int listenerConcurrency;

    @Value("${smartlogix.kafka.listener.auto-startup:true}")
    private boolean listenerAutoStartup;

    @Value("${smartlogix.kafka.retry.max-attempts:3}")
    private int retryMaxAttempts;

    // Mapper compartido: fechas ISO-8601 y tolerante a campos nuevos (evolucion de eventos)
    private static ObjectMapper eventMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .build();
    }

    // ------------------------------------------------------------------
    // PRODUCTOR: acks=all + idempotencia = sin perdida ni duplicados por reintento de red
    // ------------------------------------------------------------------
    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "usuarios-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, producerMaxBlockMs);

        JsonSerializer<Object> json = new JsonSerializer<>(eventMapper());
        json.setAddTypeInfo(false); // el consumidor conoce su DTO; no se viajan nombres de clase

        // byte[] se envia tal cual (lo usa el DLT cuando el mensaje original no se pudo deserializar)
        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(Object.class, json);

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(),
                new DelegatingByTypeSerializer(delegates, true));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    // ------------------------------------------------------------------
    // CONSUMIDOR: JSON -> DTO tipado. ErrorHandlingDeserializer evita que un
    // mensaje corrupto bloquee la particion (va directo al DLT).
    // ------------------------------------------------------------------
    @Bean
    public ConsumerFactory<String, LogisticaEventoDTO> logisticaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "usuarios-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // el commit lo hace el contenedor tras procesar
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);

        JsonDeserializer<LogisticaEventoDTO> json = new JsonDeserializer<>(LogisticaEventoDTO.class, eventMapper(), false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new ErrorHandlingDeserializer<>(json));
    }

    // ------------------------------------------------------------------
    // MANEJO DE ERRORES: 3 reintentos con backoff exponencial (1s, 2s, 4s) y
    // despues al DLT. Un payload invalido (IllegalArgumentException) no se
    // reintenta porque nunca va a funcionar: va directo al DLT.
    // ------------------------------------------------------------------
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1)); // -1 = Kafka elige la particion

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retryMaxAttempts);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(8000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) -> log.warn(
                "[KAFKA][RETRY] intento {} fallido topic={} particion={} offset={} causa={}",
                deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage()));
        return handler;
    }

    // ------------------------------------------------------------------
    // FABRICA DE LISTENERS: concurrency = particiones -> 1 hilo consumidor por particion
    // ------------------------------------------------------------------
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogisticaEventoDTO> logisticaListenerFactory(
            ConsumerFactory<String, LogisticaEventoDTO> logisticaConsumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, LogisticaEventoDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logisticaConsumerFactory);
        factory.setConcurrency(listenerConcurrency);
        factory.setAutoStartup(listenerAutoStartup);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        return factory;
    }
}
