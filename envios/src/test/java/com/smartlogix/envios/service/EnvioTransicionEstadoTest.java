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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * REGISTRO TF-02 — Test escrito ANTES de la corrección (TDD).
 *
 * Defecto detectado: cambiarEstado() aceptaba CUALQUIER transición de
 * estado, incluyendo transiciones inválidas como ENTREGADO → PENDIENTE
 * o EN_TRANSITO → PENDIENTE.
 *
 * Ciclo:
 *   1. Se escribió esta prueba → FALLÓ (rojo): no se lanzaba excepción.
 *   2. Se agregó la validación de transiciones en EnvioService.
 *   3. Se volvió a ejecutar     → PASÓ (verde).
 */
@ExtendWith(MockitoExtension.class)
class EnvioTransicionEstadoTest {

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
    void transicionValida_pendienteAAsignado_debePermitirse() {
        envio.setEstado(EstadoEnvio.PENDIENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(envio));
        when(repo.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        Envio resultado = service.cambiarEstado(1L, EstadoEnvio.ASIGNADO);

        assertEquals(EstadoEnvio.ASIGNADO, resultado.getEstado());
    }

    @Test
    void transicionValida_asignadoAEnTransito_debePermitirse() {
        envio.setEstado(EstadoEnvio.ASIGNADO);
        when(repo.findById(1L)).thenReturn(Optional.of(envio));
        when(repo.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        Envio resultado = service.cambiarEstado(1L, EstadoEnvio.EN_TRANSITO);

        assertEquals(EstadoEnvio.EN_TRANSITO, resultado.getEstado());
    }

    @Test
    void transicionInvalida_entregadoAPendiente_debeLanzarExcepcion() {
        // ENTREGADO es terminal: no puede volver a PENDIENTE
        envio.setEstado(EstadoEnvio.ENTREGADO);
        when(repo.findById(1L)).thenReturn(Optional.of(envio));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(1L, EstadoEnvio.PENDIENTE));

        assertTrue(ex.getMessage().contains("Transición de estado no permitida"));
        verify(repo, never()).save(any());
    }

    @Test
    void transicionInvalida_enTransitoAPendiente_debeLanzarExcepcion() {
        // Un envío en tránsito no puede volver a PENDIENTE
        envio.setEstado(EstadoEnvio.EN_TRANSITO);
        when(repo.findById(1L)).thenReturn(Optional.of(envio));

        assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(1L, EstadoEnvio.PENDIENTE));

        verify(repo, never()).save(any());
    }

    @Test
    void transicionInvalida_pendienteAEntregado_debeLanzarExcepcion() {
        // No se puede saltar ASIGNADO y EN_TRANSITO
        envio.setEstado(EstadoEnvio.PENDIENTE);
        when(repo.findById(1L)).thenReturn(Optional.of(envio));

        assertThrows(RuntimeException.class,
                () -> service.cambiarEstado(1L, EstadoEnvio.ENTREGADO));

        verify(repo, never()).save(any());
    }
}
