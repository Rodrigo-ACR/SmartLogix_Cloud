package com.smartlogix.envios.service;

import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.model.EstadoEnvio;
import com.smartlogix.envios.repository.EnvioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class EnvioService {

    private final EnvioRepository repo;

    /**
     * Máquina de estados del envío (patrón State simplificado):
     * define qué transiciones de negocio son válidas desde cada estado.
     *
     * PENDIENTE   → ASIGNADO | INCIDENCIA
     * ASIGNADO    → EN_TRANSITO | INCIDENCIA
     * EN_TRANSITO → ENTREGADO | INCIDENCIA
     * ENTREGADO   → (terminal)
     * INCIDENCIA  → ASIGNADO (se puede reasignar)
     */
    private static final Map<EstadoEnvio, Set<EstadoEnvio>> TRANSICIONES_VALIDAS = Map.of(
            EstadoEnvio.PENDIENTE,    Set.of(EstadoEnvio.ASIGNADO, EstadoEnvio.INCIDENCIA),
            EstadoEnvio.ASIGNADO,     Set.of(EstadoEnvio.EN_TRANSITO, EstadoEnvio.INCIDENCIA),
            EstadoEnvio.EN_TRANSITO,  Set.of(EstadoEnvio.ENTREGADO, EstadoEnvio.INCIDENCIA),
            EstadoEnvio.ENTREGADO,    Set.of(),
            EstadoEnvio.INCIDENCIA,   Set.of(EstadoEnvio.ASIGNADO));

    public EnvioService(EnvioRepository repo) {
        this.repo = repo;
    }

    public List<Envio> listarTodos() {
        return repo.findAll();
    }

    public Optional<Envio> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Envio crear(Envio e) {

        if (e.getPedidoId() == null) {
            throw new RuntimeException("El pedidoId es obligatorio");
        }

        if (e.getDireccion() == null || e.getDireccion().isBlank()) {
            throw new RuntimeException("La dirección es obligatoria");
        }

        e.setEstado(EstadoEnvio.PENDIENTE);

        if (e.getFechaEstimada() == null) {
            e.setFechaEstimada(LocalDate.now().plusDays(3));
        }

        return repo.save(e);
    }

    public Envio cambiarEstado(Long id, EstadoEnvio nuevoEstado) {

        Envio envio = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Envio no encontrado"));

        EstadoEnvio actual = envio.getEstado();

        // Validación de la máquina de estados (corrige TF-02)
        if (actual != null
                && !TRANSICIONES_VALIDAS.getOrDefault(actual, Set.of()).contains(nuevoEstado)) {
            throw new RuntimeException(
                    "Transición de estado no permitida: " + actual + " → " + nuevoEstado);
        }

        envio.setEstado(nuevoEstado);
        return repo.save(envio);
    }

    public Envio actualizar(Long id, Envio nuevo) {

        Envio existente = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Envio no encontrado"));

        existente.setDireccion(nuevo.getDireccion());
        existente.setTransportista(nuevo.getTransportista());
        existente.setFechaEstimada(nuevo.getFechaEstimada());

        return repo.save(existente);
    }

    public void eliminar(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Envio no encontrado");
        }
        repo.deleteById(id);
    }
}
