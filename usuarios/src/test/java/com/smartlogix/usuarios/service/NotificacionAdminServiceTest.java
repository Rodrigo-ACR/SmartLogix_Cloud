package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.dto.LogisticaEventoDTO;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionAdminServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private NotificacionAdminService service;

    private LogisticaEventoDTO evento(LogisticaEventoDTO.Tipo tipo) {
        return new LogisticaEventoDTO("e1", tipo, 1L, 2L, "PENDIENTE", null, 10L, "Teclado", 2, 5,
                Instant.now(), "inventario");
    }

    @Test
    void stockBajo_debeConsultarAdministradores() {
        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setCorreo("admin@smartlogix.cl");
        when(repository.findByRol(Rol.ADMIN)).thenReturn(List.of(admin));

        assertDoesNotThrow(() -> service.procesar(evento(LogisticaEventoDTO.Tipo.STOCK_BAJO)));

        verify(repository).findByRol(Rol.ADMIN);
    }

    @Test
    void stockBajoSinAdmins_noDebeFallar() {
        when(repository.findByRol(Rol.ADMIN)).thenReturn(List.of());
        assertDoesNotThrow(() -> service.procesar(evento(LogisticaEventoDTO.Tipo.STOCK_BAJO)));
    }

    @Test
    void eventoDeEnvio_noDebeNotificar() {
        service.procesar(evento(LogisticaEventoDTO.Tipo.ENVIO_CREADO));
        verifyNoInteractions(repository);
    }
}
