package com.smartlogix.usuarios.integration;

import com.smartlogix.usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración: levanta contexto completo con H2 en memoria.
 * Recorre el flujo real: register → login → actualizar → eliminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository repo;

    @BeforeEach
    void limpiarBD() {
        repo.deleteAll();
    }

    @Test
    @Order(1)
    void flujoRegisterYLogin() throws Exception {
        // 1. Registrar usuario
        mockMvc.perform(post("/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nombre": "Carlos Ruiz",
                          "correo": "carlos@correo.cl",
                          "password": "pass123",
                          "telefono": "912345678",
                          "direccion": "Av. Central 100"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 2. Login correcto
        mockMvc.perform(post("/usuarios/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "correo": "carlos@correo.cl",
                          "password": "pass123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("carlos@correo.cl"));
    }

    @Test
    @Order(2)
    void flujoRegisterCorreoDuplicado_debeRetornar400() throws Exception {
        // 1. Registrar primera vez
        mockMvc.perform(post("/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nombre": "Ana Torres",
                          "correo": "ana@correo.cl",
                          "password": "abc123"
                        }
                        """))
                .andExpect(status().isCreated());

        // 2. Registrar con mismo correo → debe fallar con 400
        mockMvc.perform(post("/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nombre": "Ana Torres 2",
                          "correo": "ana@correo.cl",
                          "password": "xyz456"
                        }
                        """))
                        .andExpect(status().isConflict());
                
    }

    @Test
    @Order(3)
    void flujoCompleto_registerActualizarEliminar() throws Exception {
        // Registrar
        mockMvc.perform(post("/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nombre": "Pedro Soto",
                          "correo": "pedro@correo.cl",
                          "password": "pass999"
                        }
                        """))
                .andExpect(status().isCreated());

        // Actualizar
        mockMvc.perform(put("/usuarios/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nombre": "Pedro Soto Actualizado",
                          "correo": "pedro@correo.cl",
                          "password": "pass999",
                          "rol": "CLIENTE",
                          "activo": true
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pedro Soto Actualizado"));

        // Eliminar
        mockMvc.perform(delete("/usuarios/3"))
                .andExpect(status().isOk());

        // Verificar que ya no existe
        mockMvc.perform(get("/usuarios/3"))
                .andExpect(status().isNotFound());
    }
}
