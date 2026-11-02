package com.smartlogix.envios.service;

import com.smartlogix.envios.dto.PedidoEventoDTO;
import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.repository.EnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioAutomaticoServiceTest {

    @Mock
    private EnvioRepository repo;

    @Mock
    private EnvioService envioService;

    @Mock
    private EnvioKafkaProducer kafkaProducer;

    private EnvioAutomaticoService service;

    @BeforeEach
    void setUp() {
        service = new EnvioAutomaticoService(repo, envioService, kafkaProducer);
        ReflectionTestUtils.setField(service, "autoCrear", true);
    }

    private PedidoEventoDTO evento(PedidoEventoDTO.Tipo tipo, String estadoNuevo, String direccion) {
        return new PedidoEventoDTO("e1", tipo, 9002L, "Ana", 10L, "Teclado", 2,
                direccion, "G1", "PENDIENTE", estadoNuevo, Instant.now(), "pedidos");
    }

    @Test
    void pedidoAprobado_debeCrearEnvioYPublicarEvento() {
        Envio creado = new Envio();
        when(repo.existsByPedidoId(9002L)).thenReturn(false);
        when(envioService.crear(any(Envio.class))).thenReturn(creado);

        service.procesarPedido(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "APROBADO", "Calle 1"));

        ArgumentCaptor<Envio> captor = ArgumentCaptor.forClass(Envio.class);
        verify(envioService).crear(captor.capture());
        assertEquals(9002L, captor.getValue().getPedidoId());
        assertEquals("Calle 1", captor.getValue().getDireccion());
        verify(kafkaProducer).publicarEnvioCreado(creado);
    }

    @Test
    void pedidoConEnvioExistente_noDebeDuplicar() {
        when(repo.existsByPedidoId(9002L)).thenReturn(true);
        service.procesarPedido(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "APROBADO", "Calle 1"));
        verifyNoInteractions(envioService, kafkaProducer);
    }

    @Test
    void estadoDistintoDeAprobado_debeIgnorarse() {
        service.procesarPedido(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "RECHAZADO", "Calle 1"));
        verifyNoInteractions(repo, envioService, kafkaProducer);
    }

    @Test
    void pedidoCreado_debeIgnorarse() {
        service.procesarPedido(evento(PedidoEventoDTO.Tipo.PEDIDO_CREADO, "PENDIENTE", "Calle 1"));
        verifyNoInteractions(repo, envioService, kafkaProducer);
    }

    @Test
    void autoCrearDeshabilitado_noDebeCrearEnvio() {
        ReflectionTestUtils.setField(service, "autoCrear", false);
        service.procesarPedido(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "APROBADO", "Calle 1"));
        verifyNoInteractions(repo, envioService, kafkaProducer);
    }

    @Test
    void sinDireccion_noDebeCrearEnvio() {
        when(repo.existsByPedidoId(9002L)).thenReturn(false);
        service.procesarPedido(evento(PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO, "APROBADO", " "));
        verify(envioService, never()).crear(any());
    }
}
