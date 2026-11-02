package com.smartlogix.kafkaadmin.service;

import com.smartlogix.kafkaadmin.exception.RecursoProtegidoException;
import org.apache.kafka.clients.admin.Admin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaAdminServiceTest {

    @Mock
    private Admin admin;

    @Mock
    private KafkaTemplate<String, String> publicador;

    private KafkaAdminService service;

    @BeforeEach
    void setUp() {
        service = new KafkaAdminService(admin, publicador, "localhost:9092", 1);
    }

    @Test
    void eliminarTopicoProtegido_sinForce_debeRechazarse() {
        assertThrows(RecursoProtegidoException.class,
                () -> service.eliminarTopico("smartlogix.pedidos.eventos", false));
        verifyNoInteractions(admin);
    }

    @Test
    void eliminarTopico_conNombreInvalido_debeRechazarse() {
        assertThrows(RuntimeException.class, () -> service.eliminarTopico("../etc/passwd", true));
        verifyNoInteractions(admin);
    }
}
