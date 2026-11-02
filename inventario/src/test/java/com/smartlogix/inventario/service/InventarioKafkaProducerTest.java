package com.smartlogix.inventario.service;

import com.smartlogix.inventario.config.KafkaConfig;
import com.smartlogix.inventario.dto.LogisticaEventoDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publicarStockBajo_debeEnviarEventoAlTopicoLogistica() {
        when(kafkaTemplate.send(eq(KafkaConfig.TOPIC_LOGISTICA), eq("PRODUCTO-10"), any()))
                .thenReturn(new CompletableFuture<SendResult<String, Object>>());
        InventarioKafkaProducer producer = new InventarioKafkaProducer(kafkaTemplate);

        producer.publicarStockBajo(10L, "Teclado", 2, 5);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_LOGISTICA), eq("PRODUCTO-10"), captor.capture());
        LogisticaEventoDTO e = (LogisticaEventoDTO) captor.getValue();
        assertEquals(LogisticaEventoDTO.Tipo.STOCK_BAJO, e.tipo());
        assertEquals(10L, e.productoId());
        assertEquals(2, e.stockActual());
        assertEquals(5, e.umbral());
        assertEquals("inventario", e.origen());
        assertNotNull(e.eventId());
    }

    @Test
    void publicarStockBajo_siKafkaFalla_noDebePropagarExcepcion() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new RuntimeException("sin broker"));
        InventarioKafkaProducer producer = new InventarioKafkaProducer(kafkaTemplate);
        assertDoesNotThrow(() -> producer.publicarStockBajo(10L, "Teclado", 2, 5));
    }
}
