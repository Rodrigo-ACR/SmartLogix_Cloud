package com.smartlogix.pedidos.controller;

import com.smartlogix.pedidos.model.EstadoPedido;
import com.smartlogix.pedidos.model.Pedido;
import com.smartlogix.pedidos.service.PedidoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias del PedidoController usando MockMvc.
 * Cubre el controller completo y el GlobalExceptionHandler
 * (mapeo de RuntimeException a 400/404/500).
 */
@WebMvcTest(PedidoController.class)
class PedidoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidoService service;

    private Pedido pedidoDemo() {
        Pedido p = new Pedido();
        p.setCliente("Juan Pérez");
        p.setProductoId(1L);
        p.setNombreProducto("Polera Azul");
        p.setCantidad(2);
        p.setEstado(EstadoPedido.CREADO);
        return p;
    }

    // ============ GET /pedidos ============

    @Test
    void listar_debeRetornar200ConLista() throws Exception {
        when(service.listarTodos()).thenReturn(List.of(pedidoDemo()));

        mvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cliente").value("Juan Pérez"))
                .andExpect(jsonPath("$[0].estado").value("CREADO"));
    }

    // ============ GET /pedidos/{id} ============

    @Test
    void obtener_existente_debeRetornar200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(Optional.of(pedidoDemo()));

        mvc.perform(get("/pedidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreProducto").value("Polera Azul"));
    }

    @Test
    void obtener_inexistente_debeRetornar404() throws Exception {
        when(service.buscarPorId(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/pedidos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Pedido no encontrado"))
                .andExpect(jsonPath("$.codigo").value(404));
    }

    // ============ POST /pedidos ============

    @Test
    void crear_valido_debeRetornar201() throws Exception {
        when(service.crear(any(Pedido.class))).thenReturn(pedidoDemo());

        mvc.perform(post("/pedidos")
                .contentType("application/json")
                .content("""
                        {"cliente":"Juan Pérez","productoId":1,
                         "nombreProducto":"Polera Azul","cantidad":2}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CREADO"));
    }

    @Test
    void crear_sinCliente_debeRetornar400PorExceptionHandler() throws Exception {
        when(service.crear(any(Pedido.class)))
                .thenThrow(new RuntimeException("El cliente es obligatorio"));

        mvc.perform(post("/pedidos")
                .contentType("application/json")
                .content("{\"productoId\":1,\"cantidad\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cliente es obligatorio"));
    }

    @Test
    void crear_cantidadInvalida_debeRetornar400() throws Exception {
        when(service.crear(any(Pedido.class)))
                .thenThrow(new RuntimeException("La cantidad debe ser mayor a 0"));

        mvc.perform(post("/pedidos")
                .contentType("application/json")
                .content("{\"cliente\":\"Juan\",\"productoId\":1,\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    // ============ PUT /pedidos/{id} ============

    @Test
    void actualizar_inexistente_debeRetornar404() throws Exception {
        when(service.actualizar(eq(99L), any(Pedido.class)))
                .thenThrow(new RuntimeException("Pedido no encontrado"));

        mvc.perform(put("/pedidos/99")
                .contentType("application/json")
                .content("{\"cliente\":\"Juan\",\"productoId\":1,\"cantidad\":3}"))
                .andExpect(status().isNotFound());
    }

    // ============ PATCH /pedidos/{id}/estado ============

    @Test
    void cambiarEstado_debeRetornar200ConNuevoEstado() throws Exception {
        Pedido aprobado = pedidoDemo();
        aprobado.setEstado(EstadoPedido.APROBADO);
        when(service.cambiarEstado(1L, EstadoPedido.APROBADO)).thenReturn(aprobado);

        mvc.perform(patch("/pedidos/1/estado").param("estado", "APROBADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    // ============ DELETE /pedidos/{id} ============

    @Test
    void eliminar_existente_debeRetornar200() throws Exception {
        mvc.perform(delete("/pedidos/1"))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_inexistente_debeRetornar404() throws Exception {
        doThrow(new RuntimeException("Pedido no encontrado"))
                .when(service).eliminar(99L);

        mvc.perform(delete("/pedidos/99"))
                .andExpect(status().isNotFound());
    }

    // ============ Error genérico → 500 ============

    @Test
    void errorInesperado_debeRetornar500() throws Exception {
        when(service.listarTodos())
                .thenThrow(new RuntimeException("fallo de conexión inesperado"));

        mvc.perform(get("/pedidos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Error interno"));
    }
}
