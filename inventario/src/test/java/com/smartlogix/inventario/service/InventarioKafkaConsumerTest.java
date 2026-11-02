package com.smartlogix.inventario.service;

import com.smartlogix.inventario.config.KafkaConfig;
import com.smartlogix.inventario.dto.PedidoEventoDTO;
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
class InventarioKafkaConsumerTest {

    @Mock
    private ReposicionStockService reposicionStockService;

    @InjectMocks
    private InventarioKafkaConsumer consumer;

    private ConsumerRecord<String, PedidoEventoDTO> registro(PedidoEventoDTO e) {
        return new ConsumerRecord<>(KafkaConfig.TOPIC_PEDIDOS, 1, 7L, "PEDIDO-1", e);
    }

    private PedidoEventoDTO evento(String id, PedidoEventoDTO.Tipo tipo, Long pedidoId) {
        return new PedidoEventoDTO(id, tipo, pedidoId, "Ana", 10L, "Teclado", 2,
                "Calle 1", "G1", "PENDIENTE", "RECHAZADO", Instant.now(), "pedidos");
    }

    @Test
    void eventoValido_debeDelegarAlServicio() {
        PedidoEventoDTO e = evento("e1", PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, 1L);
        consumer.onEventoPedido(registro(e));
        verify(reposicionStockService).procesar(e);
    }

    @Test
    void payloadNulo_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onEventoPedido(registro(null)));
        verifyNoInteractions(reposicionStockService);
    }

    @Test
    void sinEventId_debeLanzarIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> consumer.onEventoPedido(registro(evento(" ", PedidoEventoDTO.Tipo.PEDIDO_CREADO, 1L))));
        verifyNoInteractions(reposicionStockService);
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
