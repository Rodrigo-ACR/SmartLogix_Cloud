package com.smartlogix.envios.service;

import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.model.EstadoEnvio;
import com.smartlogix.envios.repository.EnvioRepository;
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
class EnvioServiceTest {

    @Mock
    private EnvioRepository repo;

    @InjectMocks
    private EnvioService service;

    private Envio envio;

    @BeforeEach
    void setUp() {
        envio = new Envio();
        envio.setPedidoId(1L);
        envio.setDireccion("Av. Siempre Viva 123");
        envio.setTransportista("Chilexpress");
    }

    @Test
    void listarTodos_debeRetornarLista() {
        when(repo.findAll()).thenReturn(List.of(envio));
        List<Envio> resultado = service.listarTodos();
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void buscarPorId_existente_debeRetornarEnvio() {
        when(repo.findById(1L)).thenReturn(Optional.of(envio));
        Optional<Envio> resultado = service.buscarPorId(1L);
        assertTrue(resultado.isPresent());
    }

    @Test
    void buscarPorId_noExistente_debeRetornarVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        Optional<Envio> resultado = service.buscarPorId(99L);
        assertFalse(resultado.isPresent());
    }

    @Test
    void crear_envioValido_debeGuardar() {
        when(repo.save(any())).thenReturn(envio);
        Envio resultado = service.crear(envio);
        assertNotNull(resultado);
        assertEquals(EstadoEnvio.PENDIENTE, resultado.getEstado());
    }

    @Test
    void crear_sinPedidoId_debeLanzarExcepcion() {
        envio.setPedidoId(null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(envio));
        assertEquals("El pedidoId es obligatorio", ex.getMessage());
    }

    @Test
    void crear_sinDireccion_debeLanzarExcepcion() {
        envio.setDireccion("");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(envio));
        assertEquals("La dirección es obligatoria", ex.getMessage());
    }

    @Test
    void cambiarEstado_existente_debeActualizar() {
        when(repo.findById(1L)).thenReturn(Optional.of(envio));
        when(repo.save(any())).thenReturn(envio);
        Envio resultado = service.cambiarEstado(1L, EstadoEnvio.EN_TRANSITO);
        assertNotNull(resultado);
        assertEquals(EstadoEnvio.EN_TRANSITO, resultado.getEstado());
    }

    @Test
    void cambiarEstado_noExistente_debeLanzarExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(99L, EstadoEnvio.EN_TRANSITO));
        assertEquals("Envio no encontrado", ex.getMessage());
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
        assertEquals("Envio no encontrado", ex.getMessage());
    }
}