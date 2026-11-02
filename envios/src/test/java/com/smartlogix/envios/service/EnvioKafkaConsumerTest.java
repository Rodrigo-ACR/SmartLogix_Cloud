package com.smartlogix.envios.service;

import com.smartlogix.envios.config.KafkaConfig;
import com.smartlogix.envios.dto.PedidoEventoDTO;
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
class EnvioKafkaConsumerTest {

    @Mock
    private EnvioAutomaticoService envioAutomaticoService;

    @InjectMocks
    private EnvioKafkaConsumer consumer;

    private ConsumerRecord<String, PedidoEventoDTO> registro(PedidoEventoDTO e) {
        return new ConsumerRecord<>(KafkaConfig.TOPIC_PEDIDOS, 0, 3L, "PEDIDO-1", e);
    }

    private PedidoEventoDTO evento(String id, PedidoEventoDTO.Tipo tipo, Long pedidoId) {
        return new PedidoEventoDTO(id, tipo, pedidoId, "Ana", 10L, "Teclado", 2,
                "Calle 1", "G1", "PENDIENTE", "APROBADO", Instant.now(), "pedidos");
    }

    @Test
    void eventoValido_debeDelegarAlServicio() {
        PedidoEventoDTO e = evento("e1", PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, 1L);
        consumer.onEventoPedido(registro(e));
        verify(envioAutomaticoService).procesarPedido(e);
    }

    @Test
    void payloadNulo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onEventoPedido(registro(null)));
        verifyNoInteractions(envioAutomaticoService);
    }

    @Test
    void sinEventId_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoPedido(registro(evento("", PedidoEventoDTO.Tipo.PEDIDO_CREADO, 1L))));
    }

    @Test
    void sinTipo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoPedido(registro(evento("e1", null, 1L))));
    }

    @Test
    void sinPedidoId_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoPedido(registro(evento("e1", PedidoEventoDTO.Tipo.PEDIDO_CREADO, null))));
    }
}
