package com.smartlogix.inventario.service;

import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository repo;
    private final StockEventProducer stockEventProducer;

    // Umbral minimo de stock: por debajo de este valor se publica una alerta
    private static final int UMBRAL_STOCK_MINIMO = 5;

    public ProductoService(ProductoRepository repo, StockEventProducer stockEventProducer) {
        this.repo = repo;
        this.stockEventProducer = stockEventProducer;
    }

    public List<Producto> listarTodos() {
        return repo.findAll();
    }

    public Optional<Producto> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Producto crear(Producto p) {

        if (p.getNombre() == null || p.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del producto es obligatorio");
        }

        if (p.getPrecio() <= 0) {
            throw new RuntimeException("El precio debe ser mayor a 0");
        }

        if (p.getStock() < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }

        if (repo.findByNombre(p.getNombre()).isPresent()) {
            throw new RuntimeException("Ya existe un producto con ese nombre");
        }

        return repo.save(p);
    }

    public Producto actualizar(Long id, Producto nuevo) {

        Producto existente = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (nuevo.getStock() < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }

        if (nuevo.getPrecio() <= 0) {
            throw new RuntimeException("El precio debe ser mayor a 0");
        }

        existente.setNombre(nuevo.getNombre());
        existente.setDescripcion(nuevo.getDescripcion());
        existente.setPrecio(nuevo.getPrecio());
        existente.setStock(nuevo.getStock());
        existente.setImagen1(nuevo.getImagen1());
        existente.setImagen2(nuevo.getImagen2());
        existente.setImagen3(nuevo.getImagen3());

        return repo.save(existente);
    }

    public void eliminar(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Producto no encontrado");
        }
        repo.deleteById(id);
    }

    /**
     * Descuenta stock validando que la cantidad sea mayor a 0 (corrige TF-04).
     * Antes se permitía descontar 0 o valores negativos sin error.
     */
    public void descontarStock(Long id, int cantidad) {

        // Validación de cantidad inválida (corrige TF-04)
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad a descontar debe ser mayor a 0");
        }

        Producto p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (p.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente");
        }

        p.setStock(p.getStock() - cantidad);
        repo.save(p);

        // Si el stock quedo por debajo del umbral minimo, se publica una
        // alerta de forma asincrona hacia RabbitMQ (Exchange tipo fanout:
        // se difunde a todos los suscriptores interesados en esta alerta).
        if (p.getStock() < UMBRAL_STOCK_MINIMO) {
            stockEventProducer.publicarStockBajo(p.getId(), p.getNombre(), p.getStock());
        }
    }
}