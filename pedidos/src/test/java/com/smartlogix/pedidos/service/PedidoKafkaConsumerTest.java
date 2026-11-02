package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.config.KafkaConfig;
import com.smartlogix.pedidos.dto.LogisticaEventoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoKafkaConsumerTest {

    @Mock
    private SeguimientoLogisticoService seguimiento;

    @InjectMocks
    private PedidoKafkaConsumer consumer;

    private ConsumerRecord<String, LogisticaEventoDTO> registro(LogisticaEventoDTO e) {
        return new ConsumerRecord<>(KafkaConfig.TOPIC_LOGISTICA, 0, 1L, "ENVIO-5", e);
    }

    private LogisticaEventoDTO evento(String id, LogisticaEventoDTO.Tipo tipo, Long envioId, Long pedidoId) {
        return new LogisticaEventoDTO(id, tipo, envioId, pedidoId, "PENDIENTE", null, null, null, null, null,
                Instant.now(), "envios");
    }

    @Test
    void envioValido_debeDelegarAlServicio() {
        LogisticaEventoDTO e = evento("e1", LogisticaEventoDTO.Tipo.ENVIO_CREADO, 5L, 9002L);
        consumer.onEventoLogistico(registro(e));
        verify(seguimiento).registrar(e);
    }

    @Test
    void payloadNulo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onEventoLogistico(registro(null)));
        verifyNoInteractions(seguimiento);
    }

    @Test
    void sinEventId_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoLogistico(registro(evento("", LogisticaEventoDTO.Tipo.ENVIO_CREADO, 5L, 9002L))));
    }

    @Test
    void sinTipo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoLogistico(registro(evento("e1", null, 5L, 9002L))));
    }

    @Test
    void envioSinIds_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoLogistico(registro(evento("e1", LogisticaEventoDTO.Tipo.ENVIO_CREADO, null, 9002L))));
    }
}
