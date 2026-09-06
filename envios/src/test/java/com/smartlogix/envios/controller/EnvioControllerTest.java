package com.smartlogix.envios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.model.EstadoEnvio;
import com.smartlogix.envios.service.EnvioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnvioController.class)
class EnvioControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private EnvioService service;

        @Autowired
        private ObjectMapper objectMapper;

        private Envio envio;

        @BeforeEach
        void setUp() {
                envio = new Envio();
                envio.setPedidoId(1L);
                envio.setDireccion("Av. Siempre Viva 123");
                envio.setTransportista("Chilexpress");
                envio.setEstado(EstadoEnvio.PENDIENTE);
                envio.setFechaEstimada(LocalDate.now().plusDays(3));
        }

        @Test
        void listar_debeRetornar200() throws Exception {
                when(service.listarTodos()).thenReturn(List.of(envio));

                mockMvc.perform(get("/envios"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].direccion").value("Av. Siempre Viva 123"));
        }

        @Test
        void obtener_existente_debeRetornar200() throws Exception {
                when(service.buscarPorId(1L)).thenReturn(Optional.of(envio));

                mockMvc.perform(get("/envios/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transportista").value("Chilexpress"));
        }

        @Test
        void obtener_noExistente_debeRetornar404() throws Exception {
                when(service.buscarPorId(99L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/envios/99"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void crear_envioValido_debeRetornar201() throws Exception {
                when(service.crear(any())).thenReturn(envio);

                mockMvc.perform(post("/envios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(envio)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        }

        @Test
        void crear_sinDireccion_debeRetornar400() throws Exception {
                when(service.crear(any()))
                                .thenThrow(new RuntimeException("La dirección es obligatoria"));

                mockMvc.perform(post("/envios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(envio)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void crear_sinPedidoId_debeRetornar400() throws Exception {
                when(service.crear(any()))
                                .thenThrow(new RuntimeException("El pedidoId es obligatorio"));

                mockMvc.perform(post("/envios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(envio)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void cambiarEstado_valido_debeRetornar200() throws Exception {
                envio.setEstado(EstadoEnvio.ASIGNADO);
                when(service.cambiarEstado(eq(1L), eq(EstadoEnvio.ASIGNADO))).thenReturn(envio);

                mockMvc.perform(patch("/envios/1/estado")
                                .param("estado", "ASIGNADO"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.estado").value("ASIGNADO"));
        }

        @Test
        void cambiarEstado_transicionInvalida_debeRetornar409() throws Exception {
                when(service.cambiarEstado(eq(1L), eq(EstadoEnvio.PENDIENTE)))
                                .thenThrow(new RuntimeException(
                                                "Transición de estado no permitida: ENTREGADO → PENDIENTE"));

                mockMvc.perform(patch("/envios/1/estado")
                                .param("estado", "PENDIENTE"))
                                .andExpect(status().isConflict());
        }

        @Test
        void actualizar_existente_debeRetornar200() throws Exception {
                when(service.actualizar(eq(1L), any())).thenReturn(envio);

                mockMvc.perform(put("/envios/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(envio)))
                                .andExpect(status().isOk());
        }

        @Test
        void actualizar_noExistente_debeRetornar404() throws Exception {
                when(service.actualizar(eq(99L), any()))
                                .thenThrow(new RuntimeException("Envio no encontrado"));

                mockMvc.perform(put("/envios/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(envio)))
                                .andExpect(status().isNotFound());
        }

        @Test
        void eliminar_existente_debeRetornar200() throws Exception {
                mockMvc.perform(delete("/envios/1"))
                                .andExpect(status().isOk());
        }
}