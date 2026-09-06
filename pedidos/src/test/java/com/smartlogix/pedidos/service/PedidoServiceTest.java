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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repo;

    @InjectMocks
    private PedidoService service;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setCliente("Juan Pérez");
        pedido.setProductoId(1L);
        pedido.setNombreProducto("Polera Azul");
        pedido.setCantidad(2);
        pedido.setEstado(EstadoPedido.CREADO);
    }

    @Test
    void listarTodos_debeRetornarLista() {
        when(repo.findAll()).thenReturn(List.of(pedido));
        List<Pedido> resultado = service.listarTodos();
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void buscarPorId_existente_debeRetornarPedido() {
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));
        Optional<Pedido> resultado = service.buscarPorId(1L);
        assertTrue(resultado.isPresent());
    }

    @Test
    void buscarPorId_noExistente_debeRetornarVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        Optional<Pedido> resultado = service.buscarPorId(99L);
        assertFalse(resultado.isPresent());
    }

    @Test
    void crear_pedidoValido_debeGuardar() {
        when(repo.save(any())).thenReturn(pedido);
        Pedido resultado = service.crear(pedido);
        assertNotNull(resultado);
        assertEquals(EstadoPedido.CREADO, resultado.getEstado());
    }

    @Test
    void crear_clienteVacio_debeLanzarExcepcion() {
        pedido.setCliente("");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(pedido));
        assertEquals("El cliente es obligatorio", ex.getMessage());
    }

    @Test
    void crear_sinProducto_debeLanzarExcepcion() {
        pedido.setProductoId(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(pedido));
        assertEquals("El producto es obligatorio", ex.getMessage());
    }

    @Test
    void crear_cantidadCero_debeLanzarExcepcion() {
        pedido.setCantidad(0);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(pedido));
        assertEquals("La cantidad debe ser mayor a 0", ex.getMessage());
    }

    @Test
    void cambiarEstado_existente_debeActualizar() {
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));
        when(repo.save(any())).thenReturn(pedido);
        Pedido resultado = service.cambiarEstado(1L, EstadoPedido.VALIDADO);
        assertNotNull(resultado);
        assertEquals(EstadoPedido.VALIDADO, resultado.getEstado());
    }

    @Test
    void cambiarEstado_noExistente_debeLanzarExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(99L, EstadoPedido.APROBADO));
        assertEquals("Pedido no encontrado", ex.getMessage());
    }

    @Test
    void eliminar_existente_debeEliminar() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);
        assertDoesNotThrow(() -> service.eliminar(1L));
        verify(repo, times(1)).deleteById(1L);
    }

    @Test
    void eliminar_noExistente_debeLanzarExcepcion() {
        when(repo.existsById(99L)).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.eliminar(99L));
        assertEquals("Pedido no encontrado", ex.getMessage());
    }
}