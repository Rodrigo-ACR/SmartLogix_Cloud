package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.dto.RegisterRequest;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;

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
 * REGISTRO TF-03 — Test escrito ANTES de la corrección (TDD).
 *
 * Defecto detectado: el servicio no validaba que el correo de un usuario
 * activo no pudiera ser modificado a uno ya existente en otro registro,
 * y no lanzaba excepción con un mensaje claro al intentar
 * registrar/actualizar con correo duplicado en ciertos flujos.
 *
 * Ciclo:
 *   1. Se escribió esta prueba → FALLÓ (rojo).
 *   2. Se agregó la validación en UsuarioService.
 *   3. Se volvió a ejecutar    → PASÓ (verde).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioValidacionTest {

    @Mock
    private UsuarioRepository repo;

    @InjectMocks
    private UsuarioService service;

    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioExistente = new Usuario();
        usuarioExistente.setNombre("Juan Pérez");
        usuarioExistente.setCorreo("juan@correo.cl");
        usuarioExistente.setPassword("1234");
        usuarioExistente.setRol(Rol.CLIENTE);
        usuarioExistente.setActivo(true);
    }

    @Test
    void register_correoNuevo_debeGuardarCorrectamente() {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("María López");
        req.setCorreo("maria@correo.cl");
        req.setPassword("abcd");
        req.setTelefono("987654321");
        req.setDireccion("Calle Nueva 1");

        when(repo.findByCorreo("maria@correo.cl")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = service.register(req);

        assertEquals(Rol.CLIENTE, resultado.getRol());
        assertEquals("maria@correo.cl", resultado.getCorreo());
        assertTrue(resultado.getActivo());
    }

    @Test
    void register_correoDuplicado_debeLanzarExcepcion() {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("Otro Usuario");
        req.setCorreo("juan@correo.cl");
        req.setPassword("5678");

        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.of(usuarioExistente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.register(req));

        assertEquals("El correo ya está registrado", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void actualizar_usuarioNoExistente_debeLanzarExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.actualizar(99L, usuarioExistente));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void login_usuarioInactivo_debeLanzarExcepcion() {
        usuarioExistente.setActivo(false);
        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.of(usuarioExistente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.login("juan@correo.cl", "1234"));

        assertTrue(ex.getMessage().contains("inactivo") || ex.getMessage().contains("inhabilitado"));
        verify(repo, never()).save(any());
    }

    @Test
    void login_passwordIncorrecta_debeLanzarExcepcionClara() {
        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.of(usuarioExistente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.login("juan@correo.cl", "wrongpassword"));

        assertEquals("Contraseña incorrecta", ex.getMessage());
    }
}
