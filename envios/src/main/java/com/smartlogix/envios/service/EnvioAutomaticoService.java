package com.smartlogix.envios.service;

import com.smartlogix.envios.dto.PedidoEventoDTO;
import com.smartlogix.envios.model.Envio;
import com.smartlogix.envios.repository.EnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Logica de dominio: cuando un pedido pasa a APROBADO se crea el envio
 * (estado PENDIENTE) sin intervencion manual. Reutiliza EnvioService.crear,
 * asi aplica las mismas validaciones que la creacion por REST.
 *
 * Es idempotente por pedidoId: si ya existe un envio para ese pedido no crea
 * otro (un evento repetido o un envio creado a mano antes no generan duplicados).
 * Se puede apagar con smartlogix.kafka.envios.auto-crear=false.
 */
@Service
public class EnvioAutomaticoService {

    private static final Logger log = LoggerFactory.getLogger(EnvioAutomaticoService.class);

    private final EnvioRepository repo;
    private final EnvioService envioService;
    private final EnvioKafkaProducer kafkaProducer;

    @Value("${smartlogix.kafka.envios.auto-crear:true}")
    private boolean autoCrear;

    public EnvioAutomaticoService(EnvioRepository repo, EnvioService envioService, EnvioKafkaProducer kafkaProducer) {
        this.repo = repo;
        this.envioService = envioService;
        this.kafkaProducer = kafkaProducer;
    }

    public void procesarPedido(PedidoEventoDTO evento) {
        boolean aprobado = evento.tipo() == PedidoEventoDTO.Tipo.PEDIDO_ESTADO_CAMBIADO
                && "APROBADO".equals(evento.estadoNuevo());
        if (!aprobado) {
            return;
        }
        if (!autoCrear) {
            log.info("[KAFKA][ENVIOS] Creacion automatica deshabilitada (pedidoId={})", evento.pedidoId());
            return;
        }
        if (repo.existsByPedidoId(evento.pedidoId())) {
            log.warn("[KAFKA][ENVIOS][IDEMPOTENCIA] El pedido {} ya tiene envio; no se crea otro", evento.pedidoId());
            return;
        }
        if (evento.direccion() == null || evento.direccion().isBlank()) {
            log.warn("[KAFKA][ENVIOS] Pedido {} aprobado sin direccion: el envio debe crearse manualmente",
                    evento.pedidoId());
            return;
        }

        Envio envio = new Envio();
        envio.setPedidoId(evento.pedidoId());
        envio.setGrupoId(evento.grupoId());
        envio.setDireccion(evento.direccion());

        Envio creado = envioService.crear(envio);
        log.info("[KAFKA][ENVIOS] Pedido {} APROBADO -> envio {} creado automaticamente (PENDIENTE)",
                evento.pedidoId(), creado.getId());

        kafkaProducer.publicarEnvioCreado(creado);
    }
}
