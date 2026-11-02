package com.smartlogix.inventario.service;

import com.smartlogix.inventario.dto.PedidoEventoDTO;
import com.smartlogix.inventario.model.EventoProcesado;
import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.repository.EventoProcesadoRepository;
import com.smartlogix.inventario.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReposicionStockServiceTest {

    @Mock
    private ProductoRepository productoRepo;

    @Mock
    private EventoProcesadoRepository eventoRepo;

    private ReposicionStockService service;
    private Producto producto;

    @BeforeEach
    void setUp() {
        service = new ReposicionStockService(productoRepo, eventoRepo);
        ReflectionTestUtils.setField(service, "reponerAlRechazar", true);
        producto = new Producto();
        producto.setNombre("Teclado");
        producto.setStock(5);
    }

    private PedidoEventoDTO evento(PedidoEventoDTO.Tipo tipo, String estadoNuevo, Long productoId, Integer cantidad) {
        return new PedidoEventoDTO("evt-1", tipo, 9002L, "Ana", productoId, "Teclado", cantidad,
                "Calle 1", "G1", "PENDIENTE", estadoNuevo, Instant.now(), "pedidos");
    }

    @Test
    void rechazado_debeReponerStockYRegistrarEvento() {
        when(eventoRepo.existsById("evt-1")).thenReturn(false);
        when(productoRepo.findById(10L)).thenReturn(Optional.of(producto));

        service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "RECHAZADO", 10L, 3));

        assertEquals(8, producto.getStock());
        verify(productoRepo).save(producto);
        verify(eventoRepo).save(any(EventoProcesado.class));
    }

    @Test
    void eventoDuplicado_noDebeReponerDosVeces() {
        when(eventoRepo.existsById("evt-1")).thenReturn(true);

        service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "RECHAZADO", 10L, 3));

        assertEquals(5, producto.getStock());
        verify(productoRepo, never()).save(any());
        verify(eventoRepo, never()).save(any());
    }

    @Test
    void estadoDistintoDeRechazado_debeIgnorarse() {
        service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "APROBADO", 10L, 3));
        verifyNoInteractions(productoRepo, eventoRepo);
    }

    @Test
    void pedidoCreado_debeIgnorarse() {
        service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_CREADO, "PENDIENTE", 10L, 3));
        verifyNoInteractions(productoRepo, eventoRepo);
    }

    @Test
    void reposicionDeshabilitada_noDebeTocarNada() {
        ReflectionTestUtils.setField(service, "reponerAlRechazar", false);
        service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "RECHAZADO", 10L, 3));
        verifyNoInteractions(productoRepo, eventoRepo);
    }

    @Test
    void cantidadInvalida_debeLanzarIllegalArgument() {
        when(eventoRepo.existsById("evt-1")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "RECHAZADO", 10L, 0)));
        verify(productoRepo, never()).save(any());
    }

    @Test
    void productoInexistente_registraEventoSinReponer() {
        when(eventoRepo.existsById("evt-1")).thenReturn(false);
        when(productoRepo.findById(10L)).thenReturn(Optional.empty());

        service.procesar(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "RECHAZADO", 10L, 3));

        verify(productoRepo, never()).save(any());
        verify(eventoRepo).save(any(EventoProcesado.class));
    }
}
