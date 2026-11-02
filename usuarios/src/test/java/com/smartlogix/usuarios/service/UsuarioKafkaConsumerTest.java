package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.config.KafkaConfig;
import com.smartlogix.usuarios.dto.LogisticaEventoDTO;
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
class UsuarioKafkaConsumerTest {

    @Mock
    private NotificacionAdminService notificacionAdminService;

    @InjectMocks
    private UsuarioKafkaConsumer consumer;

    private ConsumerRecord<String, LogisticaEventoDTO> registro(LogisticaEventoDTO e) {
        return new ConsumerRecord<>(KafkaConfig.TOPIC_LOGISTICA, 2, 11L, "PRODUCTO-10", e);
    }

    private LogisticaEventoDTO evento(String id, LogisticaEventoDTO.Tipo tipo, Long productoId) {
        return new LogisticaEventoDTO(id, tipo, null, null, null, null, productoId, "Teclado", 2, 5,
                Instant.now(), "inventario");
    }

    @Test
    void stockBajoValido_debeDelegarAlServicio() {
        LogisticaEventoDTO e = evento("e1", LogisticaEventoDTO.Tipo.STOCK_BAJO, 10L);
        consumer.onEventoLogistico(registro(e));
        verify(notificacionAdminService).procesar(e);
    }

    @Test
    void payloadNulo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onEventoLogistico(registro(null)));
        verifyNoInteractions(notificacionAdminService);
    }

    @Test
    void sinEventId_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoLogistico(registro(evento(null, LogisticaEventoDTO.Tipo.STOCK_BAJO, 10L))));
    }

    @Test
    void sinTipo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoLogistico(registro(evento("e1", null, 10L))));
    }

    @Test
    void stockBajoSinProducto_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoLogistico(registro(evento("e1", LogisticaEventoDTO.Tipo.STOCK_BAJO, null))));
    }
}
