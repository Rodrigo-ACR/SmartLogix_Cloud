package com.smartlogix.usuarios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.RegisterRequest;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UsuarioService service;

        @Autowired
        private ObjectMapper objectMapper;

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
        void listar_debeRetornar200() throws Exception {
                when(service.listarTodos()).thenReturn(List.of(usuario));

                mockMvc.perform(get("/usuarios"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].correo").value("juan@correo.cl"));
        }

        @Test
        void buscar_existente_debeRetornar200() throws Exception {
                when(service.buscarPorId(1L)).thenReturn(Optional.of(usuario));

                mockMvc.perform(get("/usuarios/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.nombre").value("Juan Pérez"));
        }

        @Test
        void buscar_noExistente_debeRetornar404() throws Exception {
                when(service.buscarPorId(99L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/usuarios/99"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void guardar_usuarioValido_debeRetornar201() throws Exception {
                when(service.guardar(any())).thenReturn(usuario);

                mockMvc.perform(post("/usuarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(usuario)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.correo").value("juan@correo.cl"));
        }

        @Test
        void actualizar_existente_debeRetornar200() throws Exception {
                when(service.actualizar(eq(1L), any())).thenReturn(usuario);

                mockMvc.perform(put("/usuarios/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(usuario)))
                                .andExpect(status().isOk());
        }

        @Test
        void actualizar_noExistente_debeRetornar404() throws Exception {
                when(service.actualizar(eq(99L), any()))
                                .thenThrow(new RuntimeException("Usuario no encontrado"));

                mockMvc.perform(put("/usuarios/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(usuario)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void eliminar_existente_debeRetornar200() throws Exception {
                mockMvc.perform(delete("/usuarios/1"))
                                .andExpect(status().isOk());
        }

        @Test
        void eliminar_noExistente_debeRetornar404() throws Exception {
                org.mockito.Mockito.doThrow(new RuntimeException("Usuario no encontrado"))
                                .when(service).eliminar(99L);

                mockMvc.perform(delete("/usuarios/99"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void login_credencialesCorrectas_debeRetornar200() throws Exception {
                LoginResponse response = new LoginResponse(1L, "Juan Pérez",
                                "juan@correo.cl", "CLIENTE", "Calle 123", "912345678");
                when(service.login("juan@correo.cl", "1234")).thenReturn(response);

                LoginRequest request = new LoginRequest();
                request.setCorreo("juan@correo.cl");
                request.setPassword("1234");

                mockMvc.perform(post("/usuarios/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.rol").value("CLIENTE"));
        }

        @Test
        void login_passwordIncorrecta_debeRetornar400() throws Exception {
                when(service.login("juan@correo.cl", "wrong"))
                                .thenThrow(new RuntimeException("Contraseña incorrecta"));

                LoginRequest request = new LoginRequest();
                request.setCorreo("juan@correo.cl");
                request.setPassword("wrong");

                mockMvc.perform(post("/usuarios/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest()); // ← cambiar aquí
        }

        @Test
        void register_correoNuevo_debeRetornar201() throws Exception {
                when(service.register(any())).thenReturn(usuario);

                RegisterRequest req = new RegisterRequest();
                req.setNombre("Juan Pérez");
                req.setCorreo("juan@correo.cl");
                req.setPassword("1234");
                req.setTelefono("912345678");
                req.setDireccion("Calle 123");

                mockMvc.perform(post("/usuarios/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated());
        }

        @Test
        void register_correoDuplicado_debeRetornar400() throws Exception {
                when(service.register(any()))
                                .thenThrow(new RuntimeException("El correo ya está registrado"));

                RegisterRequest req = new RegisterRequest();
                req.setNombre("Juan Pérez");
                req.setCorreo("juan@correo.cl");
                req.setPassword("1234");

                mockMvc.perform(post("/usuarios/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isConflict());
        }
}
