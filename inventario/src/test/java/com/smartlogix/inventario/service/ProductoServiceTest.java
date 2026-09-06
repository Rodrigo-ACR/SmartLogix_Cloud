package com.smartlogix.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

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
        producto.setDescripcion("Polera de algodón");
    }

    @Test
    void listarTodos_debeRetornarLista() {
        when(repo.findAll()).thenReturn(List.of(producto));

        List<Producto> resultado = service.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(repo, times(1)).findAll();
    }

    @Test
    void buscarPorId_existente_debeRetornarProducto() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto));

        Optional<Producto> resultado = service.buscarPorId(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Polera Azul", resultado.get().getNombre());
    }

    @Test
    void buscarPorId_noExistente_debeRetornarVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        Optional<Producto> resultado = service.buscarPorId(99L);

        assertFalse(resultado.isPresent());
    }

    @Test
    void crear_productoValido_debeGuardar() {
        when(repo.findByNombre("Polera Azul")).thenReturn(Optional.empty());
        when(repo.save(any())).thenReturn(producto);

        Producto resultado = service.crear(producto);

        assertNotNull(resultado);
        assertEquals("Polera Azul", resultado.getNombre());
        verify(repo, times(1)).save(any());
    }

    @Test
    void crear_nombreVacio_debeLanzarExcepcion() {
        producto.setNombre("");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(producto));

        assertEquals("El nombre del producto es obligatorio", ex.getMessage());
    }

    @Test
    void crear_precioNegativo_debeLanzarExcepcion() {
        producto.setPrecio(-100);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(producto));

        assertEquals("El precio debe ser mayor a 0", ex.getMessage());
    }

    @Test
    void crear_stockNegativo_debeLanzarExcepcion() {
        producto.setStock(-5);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(producto));

        assertEquals("El stock no puede ser negativo", ex.getMessage());
    }

    @Test
    void crear_nombreDuplicado_debeLanzarExcepcion() {
        when(repo.findByNombre("Polera Azul")).thenReturn(Optional.of(producto));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(producto));

        assertEquals("Ya existe un producto con ese nombre", ex.getMessage());
    }

    @Test
    void actualizar_productoExistente_debeActualizar() {
        Producto nuevo = new Producto();
        nuevo.setNombre("Polera Roja");
        nuevo.setPrecio(12000);
        nuevo.setStock(30);

        when(repo.findById(1L)).thenReturn(Optional.of(producto));
        when(repo.save(any())).thenReturn(producto);

        Producto resultado = service.actualizar(1L, nuevo);

        assertNotNull(resultado);
        verify(repo, times(1)).save(any());
    }

    @Test
    void actualizar_stockNegativo_debeLanzarExcepcion() {
        Producto nuevo = new Producto();
        nuevo.setNombre("Polera Roja");
        nuevo.setPrecio(12000);
        nuevo.setStock(-1);

        when(repo.findById(1L)).thenReturn(Optional.of(producto));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.actualizar(1L, nuevo));

        assertEquals("El stock no puede ser negativo", ex.getMessage());
    }

    @Test
    void actualizar_noExistente_debeLanzarExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.actualizar(99L, producto));

        assertEquals("Producto no encontrado", ex.getMessage());
    }

    @Test
    void eliminar_productoExistente_debeEliminar() {
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

        assertEquals("Producto no encontrado", ex.getMessage());
    }

    @Test
    void descontarStock_suficiente_debeDescontar() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto));
        when(repo.save(any())).thenReturn(producto);

        assertDoesNotThrow(() -> service.descontarStock(1L, 10));
        assertEquals(40, producto.getStock());
    }

    @Test
    void descontarStock_insuficiente_debeLanzarExcepcion() {
        when(repo.findById(1L)).thenReturn(Optional.of(producto));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.descontarStock(1L, 100));

        assertEquals("Stock insuficiente", ex.getMessage());
    }
}