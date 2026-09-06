package com.smartlogix.pedidos.integration;

import com.smartlogix.pedidos.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PRUEBA DE INTEGRACIÓN del microservicio Pedidos.
 *
 * A diferencia de las pruebas unitarias (que mockean el repositorio),
 * aquí se levanta el contexto COMPLETO de Spring: controller real,
 * service real, repositorio JPA real y base de datos H2 en memoria.
 * Se valida el flujo de negocio core: crear pedido → consultar →
 * aprobar → actualizar → eliminar, atravesando todas las capas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PedidoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PedidoRepository repo;

    @BeforeEach
    void limpiarBase() {
        repo.deleteAll();
    }

    private String crearPedidoYObtenerId() throws Exception {
        MvcResult res = mvc.perform(post("/pedidos")
                .contentType("application/json")
                .content("""
                        {"cliente":"Rodrigo","productoId":1,
                         "nombreProducto":"Polera Azul","cantidad":3}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        // extrae el valor de "id" del JSON de respuesta
        return body.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    @Order(1)
    void flujoCompleto_crearConsultarAprobar() throws Exception {
        // 1. Crear: el service debe asignar fecha y estado CREADO
        String id = crearPedidoYObtenerId();

        // 2. Consultar: el pedido quedó persistido en la BD (H2)
        mvc.perform(get("/pedidos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Rodrigo"))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.fecha").exists());

        // 3. Validar y aprobar: las transiciones siguen la máquina de
        //    estados (CREADO → VALIDADO → APROBADO) y persisten en la BD
        mvc.perform(patch("/pedidos/" + id + "/estado")
                .param("estado", "VALIDADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADO"));

        mvc.perform(patch("/pedidos/" + id + "/estado")
                .param("estado", "APROBADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        // 3b. Transición inválida: un pedido APROBADO no puede volver a CREADO
        mvc.perform(patch("/pedidos/" + id + "/estado")
                .param("estado", "CREADO"))
                .andExpect(status().isConflict());

        // 4. Releer desde la BD para confirmar la persistencia real
        mvc.perform(get("/pedidos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    @Order(2)
    void crearPedidoInvalido_atraviesaCapasYDevuelve400() throws Exception {
        // El error nace en el service real y lo traduce el handler real
        mvc.perform(post("/pedidos")
                .contentType("application/json")
                .content("{\"cliente\":\"Rodrigo\",\"productoId\":1,\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La cantidad debe ser mayor a 0"));

        // Y nada quedó persistido
        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Order(3)
    void actualizarYEliminar_persistenEnBase() throws Exception {
        String id = crearPedidoYObtenerId();

        // Actualizar cantidad
        mvc.perform(put("/pedidos/" + id)
                .contentType("application/json")
                .content("""
                        {"cliente":"Rodrigo","productoId":1,
                         "nombreProducto":"Polera Azul","cantidad":5}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(5));

        // Eliminar y verificar que ya no existe
        mvc.perform(delete("/pedidos/" + id))
                .andExpect(status().isOk());

        mvc.perform(get("/pedidos/" + id))
                .andExpect(status().isNotFound());
    }
}
