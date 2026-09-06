package com.smartlogix.envios.integration;

import com.smartlogix.envios.repository.EnvioRepository;

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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración: levanta contexto completo con H2 en memoria.
 * Recorre el flujo real: crear → consultar → cambiar estado → validar
 * transición inválida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnvioIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EnvioRepository repo;

  @BeforeEach
  void limpiarBD() {
    repo.deleteAll();
  }

  @Test
  @Order(1)
  void flujoCompleto_crearYConsultar() throws Exception {
    // 1. Crear envío
    MvcResult result = mockMvc.perform(post("/envios")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "pedidoId": 1,
              "direccion": "Av. Principal 456",
              "transportista": "StarKen"
            }
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andReturn();

    // 2. Consultar el envío creado
    mockMvc.perform(get("/envios/1"))
        .andExpect(status().isOk());
  }

  @Test
  @Order(2)
  void flujoEstados_transicionesValidas() throws Exception {
    // Crear envío
    mockMvc.perform(post("/envios")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "pedidoId": 2,
              "direccion": "Calle Los Olmos 789",
              "transportista": "Chilexpress"
            }
            """))
        .andExpect(status().isCreated());

    // PENDIENTE → ASIGNADO (válido)
    mockMvc.perform(patch("/envios/2/estado")
        .param("estado", "ASIGNADO"))
        .andExpect(status().isOk());

    // ASIGNADO → EN_TRANSITO (válido)
    mockMvc.perform(patch("/envios/2/estado")
        .param("estado", "EN_TRANSITO"))
        .andExpect(status().isOk());

    // EN_TRANSITO → PENDIENTE (inválido — debe retornar 409)
    mockMvc.perform(patch("/envios/2/estado")
        .param("estado", "PENDIENTE"))
        .andExpect(status().isConflict());
  }

  @Test
  @Order(3)
  void flujoCompleto_crearActualizarEliminar() throws Exception {
    // Crear
    mockMvc.perform(post("/envios")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "pedidoId": 3,
              "direccion": "Pasaje Verde 100",
              "transportista": "CorreosChile"
            }
            """))
        .andExpect(status().isCreated());

    // Actualizar
    mockMvc.perform(put("/envios/3")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "pedidoId": 3,
              "direccion": "Pasaje Verde 200",
              "transportista": "StarKen"
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transportista").value("StarKen"));

    // Eliminar
    mockMvc.perform(delete("/envios/3"))
        .andExpect(status().isOk());

    // Verificar que ya no existe
    mockMvc.perform(get("/envios/3"))
        .andExpect(status().isNotFound());
  }
}