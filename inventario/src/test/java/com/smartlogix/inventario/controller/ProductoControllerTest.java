package com.smartlogix.inventario.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.service.ProductoService;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService service;

    @Autowired
    private ObjectMapper objectMapper;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setNombre("Polera Azul");
        producto.setPrecio(9990);
        producto.setStock(50);
        producto.setDescripcion("Polera de algodón");
    }

    @Test
    void listar_debeRetornar200() throws Exception {
        when(service.listarTodos()).thenReturn(List.of(producto));

        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Polera Azul"));
    }

    @Test
    void obtener_existente_debeRetornar200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(Optional.of(producto));

        mockMvc.perform(get("/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Polera Azul"));
    }

    @Test
    void obtener_noExistente_debeRetornar404() throws Exception {
        when(service.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/productos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_productoValido_debeRetornar201() throws Exception {
        when(service.crear(any())).thenReturn(producto);

        mockMvc.perform(post("/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Polera Azul"));
    }

    @Test
    void crear_stockNegativo_debeRetornar400() throws Exception {
        when(service.crear(any()))
                .thenThrow(new RuntimeException("El stock no puede ser negativo"));

        mockMvc.perform(post("/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_existente_debeRetornar200() throws Exception {
        when(service.actualizar(eq(1L), any())).thenReturn(producto);

        mockMvc.perform(put("/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(producto)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_existente_debeRetornar200() throws Exception {
        mockMvc.perform(delete("/productos/1"))
                .andExpect(status().isOk());
    }
}