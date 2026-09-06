package com.smartlogix.inventario.service;

import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.repository.ProductoRepository;

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
 * REGISTRO TF-04 — Test escrito ANTES de la corrección (TDD).
 *
 * Defecto detectado: descontarStock() no validaba que la cantidad a
 * descontar sea mayor a 0, permitiendo descuentos de 0 o negativos
 * que corrompen silenciosamente el stock.
 *
 * Ciclo:
 *   1. Se escribió esta prueba → FALLÓ (rojo): no se lanzaba excepción.
 *   2. Se agregó la validación en ProductoService.
 *   3. Se volvió a ejecutar    → PASÓ (verde).
 */
@ExtendWith(MockitoExtension.class)
class ProductoStockTest {

    @Mock
    private ProductoRepository repo;

    @InjectMocks
    private ProductoService service;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setNombre("Polera Azul");
        producto.setPrecio(9990);
        producto.setStock(50);
    }

    @Test
    void descontarStock_cantidadValida_debeDescontar() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.descontarStock(1L, 10);

        assertEquals(40, producto.getStock());
        verify(repo, times(1)).save(producto);
    }

    @Test
    void descontarStock_insuficiente_debeLanzarExcepcion() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.descontarStock(1L, 100));

        assertEquals("Stock insuficiente", ex.getMessage());
        verify(repo, never()).save(any());
    }

@Test
void descontarStock_cantidadCero_debeLanzarExcepcion() {
    RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.descontarStock(1L, 0));

    assertTrue(ex.getMessage().contains("mayor a 0") || ex.getMessage().contains("inválida"));
    verify(repo, never()).save(any());
}

@Test
void descontarStock_cantidadNegativa_debeLanzarExcepcion() {
    RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.descontarStock(1L, -5));

    assertTrue(ex.getMessage().contains("mayor a 0") || ex.getMessage().contains("inválida"));
    verify(repo, never()).save(any());
}

    @Test
    void descontarStock_productoNoExistente_debeLanzarExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.descontarStock(99L, 5));

        assertEquals("Producto no encontrado", ex.getMessage());
    }
}
