package com.smartlogix.inventario.service;

import com.smartlogix.inventario.dto.PedidoEventoDTO;
import com.smartlogix.inventario.model.EventoProcesado;
import com.smartlogix.inventario.model.Producto;
import com.smartlogix.inventario.repository.EventoProcesadoRepository;
import com.smartlogix.inventario.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Logica de dominio: compensacion de stock. El BFF descuenta el stock al
 * crear un pedido; si ese pedido termina RECHAZADO, el stock debe volver.
 * Es el patron "saga con compensacion": en vez de que Pedidos llame por REST a
 * Inventario, Inventario reacciona al evento PEDIDO_ESTADO_CAMBIADO.
 *
 * Es idempotente: el eventId se registra en la misma transaccion que el
 * cambio de stock, asi un evento repetido no repone dos veces.
 */
@Service
public class ReposicionStockService {

    private static final Logger log = LoggerFactory.getLogger(ReposicionStockService.class);

    private final ProductoRepository productoRepo;
    private final EventoProcesadoRepository eventoRepo;

    @Value("${smartlogix.kafka.inventario.reponer-al-rechazar:true}")
    private boolean reponerAlRechazar;

    public ReposicionStockService(ProductoRepository productoRepo, EventoProcesadoRepository eventoRepo) {
        this.productoRepo = productoRepo;
        this.eventoRepo = eventoRepo;
    }

    @Transactional
    public void procesar(PedidoEventoDTO evento) {
        boolean esRechazo = evento.tipo() == PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO
                && "RECHAZADO".equals(evento.estadoNuevo());
        if (!esRechazo) {
            return; // a Inventario solo le importan los pedidos rechazados
        }
        if (!reponerAlRechazar) {
            log.info("[KAFKA][INVENTARIO] Reposicion por rechazo deshabilitada (pedidoId={})", evento.pedidoId());
            return;
        }
        if (eventoRepo.existsById(evento.eventId())) {
            log.warn("[KAFKA][INVENTARIO][IDEMPOTENCIA] Evento duplicado descartado eventId={}", evento.eventId());
            return;
        }
        if (evento.productoId() == null || evento.cantidad() == null || evento.cantidad() <= 0) {
            throw new IllegalArgumentException(
                    "Evento invalido: productoId y cantidad (>0) son obligatorios para reponer stock");
        }

        Optional<Producto> producto = productoRepo.findById(evento.productoId());
        if (producto.isEmpty()) {
            log.warn("[KAFKA][INVENTARIO] Pedido {} rechazado pero el producto {} ya no existe; nada que reponer",
                    evento.pedidoId(), evento.productoId());
        } else {
            Producto p = producto.get();
            int antes = p.getStock();
            p.setStock(antes + evento.cantidad());
            productoRepo.save(p);
            log.info("[KAFKA][INVENTARIO] Pedido {} RECHAZADO -> stock repuesto producto={} ({}): {} -> {}",
                    evento.pedidoId(), p.getId(), p.getNombre(), antes, p.getStock());
        }

        eventoRepo.save(new EventoProcesado(evento.eventId(), evento.tipo().name()));
    }
}
