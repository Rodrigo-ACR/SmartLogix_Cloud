package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.dto.LogisticaEventoDTO;
import com.smartlogix.pedidos.model.Pedido;
import com.smartlogix.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeguimientoLogisticoServiceTest {

    @Mock
    private PedidoRepository repo;

    @InjectMocks
    private SeguimientoLogisticoService service;

    private LogisticaEventoDTO evento(LogisticaEventoDTO.Tipo tipo) {
        return new LogisticaEventoDTO("e1", tipo, 5L, 9002L, "PENDIENTE", null, null, null, null, null,
                Instant.now(), "envios");
    }

    @Test
    void envioCreado_debeBuscarElPedido() {
        when(repo.findById(9002L)).thenReturn(Optional.of(new Pedido()));
        assertDoesNotThrow(() -> service.registrar(evento(LogisticaEventoDTO.Tipo.ENVIO_CREADO)));
        verify(repo).findById(9002L);
    }

    @Test
    void pedidoInexistente_noDebeFallar() {
        when(repo.findById(9002L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.registrar(evento(LogisticaEventoDTO.Tipo.ENVIO_ESTADO_CAMBIADO)));
    }

    @Test
    void stockBajo_debeIgnorarse() {
        service.registrar(evento(LogisticaEventoDTO.Tipo.STOCK_BAJO));
        verifyNoInteractions(repo);
    }
}
