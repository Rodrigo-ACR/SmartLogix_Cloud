package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.model.EstadoPedido;
import com.smartlogix.pedidos.model.Pedido;
import com.smartlogix.pedidos.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * REGISTRO TF-01 — Test escrito ANTES de la corrección (TDD).
 *
 * Defecto detectado: cambiarEstado() aceptaba CUALQUIER transición de
 * estado, incluyendo transiciones de negocio inválidas como
 * RECHAZADO → APROBADO o EN_PREPARACION → CREADO.
 *
 * Ciclo:
 *   1. Se escribió esta prueba  → FALLÓ (rojo): no se lanzaba excepción.
 *   2. Se agregó la validación de transiciones en PedidoService.
 *   3. Se volvió a ejecutar     → PASÓ (verde).
 */
@ExtendWith(MockitoExtension.class)
class PedidoTransicionEstadoTest {

    @Mock
    private PedidoRepository repo;

    @InjectMocks
    private PedidoService service;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setCliente("Rodrigo");
        pedido.setProductoId(1L);
        pedido.setCantidad(2);
    }

    @Test
    void transicionValida_creadoAValidado_debePermitirse() {
        pedido.setEstado(EstadoPedido.CREADO);
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));
        when(repo.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido resultado = service.cambiarEstado(1L, EstadoPedido.VALIDADO);

        assertEquals(EstadoPedido.VALIDADO, resultado.getEstado());
    }

    @Test
    void transicionValida_validadoAAprobado_debePermitirse() {
        pedido.setEstado(EstadoPedido.VALIDADO);
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));
        when(repo.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido resultado = service.cambiarEstado(1L, EstadoPedido.APROBADO);

        assertEquals(EstadoPedido.APROBADO, resultado.getEstado());
    }

    @Test
    void transicionInvalida_rechazadoAAprobado_debeLanzarExcepcion() {
        // Un pedido RECHAZADO es terminal: no puede pasar a APROBADO
        pedido.setEstado(EstadoPedido.RECHAZADO);
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(1L, EstadoPedido.APROBADO));

        assertTrue(ex.getMessage().contains("Transición de estado no permitida"));
        verify(repo, never()).save(any());
    }

    @Test
    void transicionInvalida_enPreparacionACreado_debeLanzarExcepcion() {
        // Un pedido en preparación no puede volver a CREADO
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(1L, EstadoPedido.CREADO));

        verify(repo, never()).save(any());
    }

    @Test
    void transicionInvalida_creadoAEnPreparacion_debeLanzarExcepcion() {
        // No se puede saltar VALIDADO y APROBADO
        pedido.setEstado(EstadoPedido.CREADO);
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(1L, EstadoPedido.EN_PREPARACION));

        verify(repo, never()).save(any());
    }
}
