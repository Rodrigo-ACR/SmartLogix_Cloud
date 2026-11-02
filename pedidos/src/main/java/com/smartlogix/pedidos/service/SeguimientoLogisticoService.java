package com.smartlogix.pedidos.service;

import com.smartlogix.pedidos.dto.LogisticaEventoDTO;
import com.smartlogix.pedidos.model.Pedido;
import com.smartlogix.pedidos.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Logica de dominio: relaciona los eventos de Envios con el pedido original
 * para dejar trazabilidad del despacho. No sabe nada de Kafka (eso vive en
 * PedidoKafkaConsumer), asi se puede probar y reutilizar por separado.
 */
@Service
public class SeguimientoLogisticoService {

    private static final Logger log = LoggerFactory.getLogger(SeguimientoLogisticoService.class);

    private final PedidoRepository repo;

    public SeguimientoLogisticoService(PedidoRepository repo) {
        this.repo = repo;
    }

    public void registrar(LogisticaEventoDTO evento) {
        if (evento.tipo() != LogisticaEventoDTO.Tipo.ENVIO_CREADO
                && evento.tipo() != LogisticaEventoDTO.Tipo.ENVIO_ESTADO_CAMBIADO) {
            return; // las alertas de stock no le interesan a Pedidos
        }

        Optional<Pedido> pedido = repo.findById(evento.pedidoId());
        if (pedido.isEmpty()) {
            log.warn("[KAFKA][PEDIDOS] Evento de envio {} apunta a un pedido inexistente (pedidoId={})",
                    evento.envioId(), evento.pedidoId());
            return;
        }

        log.info("[KAFKA][PEDIDOS] Seguimiento: pedido {} (cliente {}) -> envio {} esta en estado {}",
                pedido.get().getId(), pedido.get().getCliente(), evento.envioId(), evento.estado());
    }
}
