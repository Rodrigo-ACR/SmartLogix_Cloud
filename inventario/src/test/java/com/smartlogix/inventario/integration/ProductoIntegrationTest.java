package com.smartlogix.inventario.integration;

import com.smartlogix.inventario.repository.ProductoRepository;

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
 * Recorre el flujo real: crear → consultar → actualizar → eliminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductoIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ProductoRepository repo;

        @BeforeEach
        void limpiarBD() {
                repo.deleteAll();
        }

        @Test
        @Order(1)
        void flujoCompleto_crearYConsultar() throws Exception {
                // Crear producto
                mockMvc.perform(post("/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "nombre": "Polera Roja",
                                                  "descripcion": "Polera de algodón roja",
                                                  "precio": 12000,
                                                  "stock": 30
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.nombre").value("Polera Roja"));

                // Consultar lista
                mockMvc.perform(get("/productos"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].nombre").value("Polera Roja"));
        }

        @Test
        @Order(2)
        void flujoNombreDuplicado_debeRetornar400() throws Exception {
                // Crear primero
                mockMvc.perform(post("/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "nombre": "Jeans Azul",
                                                  "precio": 25000,
                                                  "stock": 15
                                                }
                                                """))
                                .andExpect(status().isCreated());

                // Crear con mismo nombre → debe fallar
                mockMvc.perform(post("/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "nombre": "Jeans Azul",
                                                  "precio": 26000,
                                                  "stock": 10
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @Order(3)
        void flujoCompleto_crearActualizarEliminar() throws Exception {
                // Crear
                mockMvc.perform(post("/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "nombre": "Zapatilla Blanca",
                                                  "precio": 45000,
                                                  "stock": 20
                                                }
                                                """))
                                .andExpect(status().isCreated());

                // Actualizar
                mockMvc.perform(put("/productos/3")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "nombre": "Zapatilla Blanca Premium",
                                                  "precio": 50000,
                                                  "stock": 25
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.nombre").value("Zapatilla Blanca Premium"));

                // Eliminar
                mockMvc.perform(delete("/productos/3"))
                                .andExpect(status().isOk());

                // Verificar que ya no existe
                mockMvc.perform(get("/productos/3"))
                                .andExpect(status().isNotFound());
        }
}
