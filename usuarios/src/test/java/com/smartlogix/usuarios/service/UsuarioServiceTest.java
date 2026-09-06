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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repo;

    @InjectMocks
    private UsuarioService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setNombre("Juan Pérez");
        usuario.setCorreo("juan@correo.cl");
        usuario.setPassword("1234");
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
    }

    @Test
    void listarTodos_debeRetornarLista() {
        when(repo.findAll()).thenReturn(List.of(usuario));
        List<Usuario> resultado = service.listarTodos();
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void buscarPorId_existente_debeRetornarUsuario() {
        when(repo.findById(1L)).thenReturn(Optional.of(usuario));
        Optional<Usuario> resultado = service.buscarPorId(1L);
        assertTrue(resultado.isPresent());
        assertEquals("Juan Pérez", resultado.get().getNombre());
    }

    @Test
    void buscarPorId_noExistente_debeRetornarVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        Optional<Usuario> resultado = service.buscarPorId(99L);
        assertFalse(resultado.isPresent());
    }

    @Test
    void register_correoNuevo_debeGuardar() {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("Juan Pérez");
        req.setCorreo("juan@correo.cl");
        req.setPassword("1234");
        req.setTelefono("912345678");
        req.setDireccion("Calle 123");

        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.empty());
        when(repo.save(any())).thenReturn(usuario);

        Usuario resultado = service.register(req);
        assertNotNull(resultado);
        assertEquals(Rol.CLIENTE, resultado.getRol());
    }

    @Test
    void register_correoDuplicado_debeLanzarExcepcion() {
        RegisterRequest req = new RegisterRequest();
        req.setNombre("Juan Pérez");
        req.setCorreo("juan@correo.cl");
        req.setPassword("1234");

        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.of(usuario));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.register(req));
        assertEquals("El correo ya está registrado", ex.getMessage());
    }

    @Test
    void login_credencialesCorrectas_debeRetornarLoginResponse() {
        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.of(usuario));

        var resultado = service.login("juan@correo.cl", "1234");

        assertNotNull(resultado);
        assertEquals("juan@correo.cl", resultado.getCorreo());
    }

    @Test
    void login_passwordIncorrecta_debeLanzarExcepcion() {
        when(repo.findByCorreo("juan@correo.cl")).thenReturn(Optional.of(usuario));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.login("juan@correo.cl", "wrongpassword"));
        assertEquals("Contraseña incorrecta", ex.getMessage());
    }

    @Test
    void login_usuarioNoExiste_debeLanzarExcepcion() {
        when(repo.findByCorreo("noexiste@correo.cl")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.login("noexiste@correo.cl", "1234"));
        assertEquals("Usuario no encontrado", ex.getMessage());
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
        assertEquals("Usuario no encontrado", ex.getMessage());
    }
}