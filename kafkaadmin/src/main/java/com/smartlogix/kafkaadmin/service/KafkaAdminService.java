package com.smartlogix.kafkaadmin.service;

import com.smartlogix.kafkaadmin.dto.CrearTopicoRequest;
import com.smartlogix.kafkaadmin.exception.KafkaNoDisponibleException;
import com.smartlogix.kafkaadmin.exception.RecursoProtegidoException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Operaciones de administracion sobre el cluster Kafka (EV3):
 *  - cluster: brokers y controlador
 *  - topicos: crear, listar, describir, modificar configuracion, aumentar particiones, eliminar
 *  - consumer groups: listar, describir (miembros y lag), eliminar
 *  - mensajes: ver los ultimos de un topico (util para inspeccionar un DLT) y publicar uno de prueba
 *
 * Usa el AdminClient oficial de Kafka. Todas las llamadas tienen un tiempo
 * maximo de espera para que la API no se cuelgue si el cluster no responde.
 */
@Service
public class KafkaAdminService {

    // Configuraciones de topico que interesan mostrar (el resto son defaults del broker)
    private static final Set<String> CONFIGS_RELEVANTES = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "segment.bytes", "max.message.bytes", "compression.type");

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._\\-]{1,200}$");

    private final Admin admin;
    private final KafkaTemplate<String, String> publicador;
    private final String bootstrapServers;
    private final long timeoutSegundos;

    public KafkaAdminService(Admin admin,
                             KafkaTemplate<String, String> publicador,
                             @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                             @Value("${kafka.admin.timeout-seconds:15}") long timeoutSegundos) {
        this.admin = admin;
        this.publicador = publicador;
        this.bootstrapServers = bootstrapServers;
        this.timeoutSegundos = timeoutSegundos;
    }

    // ==================================================================
    // CLUSTER
    // ==================================================================
    public Map<String, Object> cluster() {
        DescribeClusterResult resultado = admin.describeCluster();
        String clusterId = esperar(resultado.clusterId());
        Node controlador = esperar(resultado.controller());
        Collection<Node> nodos = esperar(resultado.nodes());

        List<Map<String, Object>> brokers = new ArrayList<>();
        nodos.stream().sorted(Comparator.comparingInt(Node::id)).forEach(n -> {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("id", n.id());
            b.put("host", n.host());
            b.put("puerto", n.port());
            b.put("esControlador", controlador != null && controlador.id() == n.id());
            brokers.add(b);
        });

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("clusterId", clusterId);
        salida.put("controladorId", controlador != null ? controlador.id() : null);
        salida.put("totalBrokers", brokers.size());
        salida.put("brokers", brokers);
        salida.put("bootstrapServers", bootstrapServers);
        return salida;
    }

    /** Foto completa: cluster + topicos + grupos (la consume la vista de demo). */
    public Map<String, Object> overview() {
        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("cluster", cluster());
        salida.put("topicos", listarTopicos());
        salida.put("grupos", listarGrupos());
        return salida;
    }

    // ==================================================================
    // TOPICOS
    // ==================================================================
    public List<Map<String, Object>> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());

        List<Map<String, Object>> salida = new ArrayList<>();
        descripciones.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .forEach(td -> {
                    int replicas = td.partitions().isEmpty() ? 0 : td.partitions().get(0).replicas().size();
                    long subReplicadas = td.partitions().stream()
                            .filter(p -> p.isr().size() < p.replicas().size()).count();
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("nombre", td.name());
                    fila.put("particiones", td.partitions().size());
                    fila.put("replicas", replicas);
                    fila.put("particionesSubReplicadas", subReplicadas);
                    fila.put("protegido", RecursosSistema.topicoProtegido(td.name()));
                    salida.add(fila);
                });
        return salida;
    }

    public Map<String, Object> describirTopico(String nombre) {
        TopicDescription td = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        if (td == null) {
            throw new UnknownTopicOrPartitionException("El topico no existe: " + nombre);
        }

        // Offsets inicial y final por particion (mensajes aproximados = final - inicial)
        Map<TopicPartition, OffsetSpec> pedidoFinal = new HashMap<>();
        Map<TopicPartition, OffsetSpec> pedidoInicial = new HashMap<>();
        for (TopicPartitionInfo p : td.partitions()) {
            TopicPartition tp = new TopicPartition(nombre, p.partition());
            pedidoFinal.put(tp, OffsetSpec.latest());
            pedidoInicial.put(tp, OffsetSpec.earliest());
        }
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales = esperar(admin.listOffsets(pedidoFinal).all());
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> iniciales = esperar(admin.listOffsets(pedidoInicial).all());

        long totalMensajes = 0;
        List<Map<String, Object>> particiones = new ArrayList<>();
        for (TopicPartitionInfo p : td.partitions()) {
            TopicPartition tp = new TopicPartition(nombre, p.partition());
            long fin = finales.get(tp).offset();
            long inicio = iniciales.get(tp).offset();
            totalMensajes += (fin - inicio);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("particion", p.partition());
            fila.put("lider", p.leader() != null ? p.leader().id() : null);
            fila.put("replicas", p.replicas().stream().map(Node::id).toList());
            fila.put("isr", p.isr().stream().map(Node::id).toList());
            fila.put("offsetInicial", inicio);
            fila.put("offsetFinal", fin);
            particiones.add(fila);
        }

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("nombre", td.name());
        salida.put("protegido", RecursosSistema.topicoProtegido(nombre));
        salida.put("totalParticiones", td.partitions().size());
        salida.put("mensajesAproximados", totalMensajes);
        salida.put("particiones", particiones);
        salida.put("configuracion", configuracionRelevante(nombre));
        return salida;
    }

    public void crearTopico(CrearTopicoRequest request) {
        validarNombre(request.getNombre());
        NewTopic nuevo = new NewTopic(request.getNombre(), request.getParticiones(), (short) request.getReplicas());
        if (request.getConfigs() != null && !request.getConfigs().isEmpty()) {
            nuevo.configs(request.getConfigs());
        }
        esperar(admin.createTopics(List.of(nuevo)).all());
    }

    public Map<String, Object> actualizarConfig(String nombre, Map<String, String> configs) {
        validarNombre(nombre);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);

        List<AlterConfigOp> operaciones = new ArrayList<>();
        configs.forEach((clave, valor) ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));

        Map<ConfigResource, Collection<AlterConfigOp>> cambios = new HashMap<>();
        cambios.put(recurso, operaciones);
        esperar(admin.incrementalAlterConfigs(cambios).all());

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("status", "actualizado");
        salida.put("nombre", nombre);
        salida.put("configuracion", configuracionRelevante(nombre));
        return salida;
    }

    public void aumentarParticiones(String nombre, int total) {
        validarNombre(nombre);
        Map<String, NewPartitions> cambios = new HashMap<>();
        cambios.put(nombre, NewPartitions.increaseTo(total));
        esperar(admin.createPartitions(cambios).all());
    }

    public void eliminarTopico(String nombre, boolean force) {
        validarNombre(nombre);
        if (RecursosSistema.topicoProtegido(nombre) && !force) {
            throw new RecursoProtegidoException("El topico '" + nombre
                    + "' lo usan los microservicios; para eliminarlo confirme con force=true");
        }
        esperar(admin.deleteTopics(List.of(nombre)).all());
    }

    private Map<String, String> configuracionRelevante(String nombre) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);
        Map<String, String> salida = new TreeMap<>();
        if (config != null) {
            for (ConfigEntry entrada : config.entries()) {
                if (CONFIGS_RELEVANTES.contains(entrada.name())) {
                    salida.put(entrada.name(), entrada.value());
                }
            }
        }
        return salida;
    }

    // ==================================================================
    // CONSUMER GROUPS
    // ==================================================================
    public List<Map<String, Object>> listarGrupos() {
        Collection<ConsumerGroupListing> grupos = esperar(admin.listConsumerGroups().all());
        List<Map<String, Object>> salida = new ArrayList<>();
        grupos.stream()
                .sorted(Comparator.comparing(ConsumerGroupListing::groupId))
                .forEach(g -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("grupo", g.groupId());
                    fila.put("estado", g.state().map(Object::toString).orElse("DESCONOCIDO"));
                    salida.add(fila);
                });
        return salida;
    }

    public Map<String, Object> describirGrupo(String grupoId) {
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(grupoId)).all()).get(grupoId);
        if (descripcion == null || "dead".equalsIgnoreCase(String.valueOf(descripcion.state()))) {
            throw new org.apache.kafka.common.errors.GroupIdNotFoundException("El grupo no existe: " + grupoId);
        }

        // Miembros y particiones que atiende cada uno (muestra como se reparte la carga)
        List<Map<String, Object>> miembros = new ArrayList<>();
        for (MemberDescription m : descripcion.members()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("consumerId", m.consumerId());
            fila.put("clientId", m.clientId());
            fila.put("host", m.host());
            fila.put("particiones", m.assignment().topicPartitions().stream()
                    .map(tp -> tp.topic() + "-" + tp.partition()).sorted().toList());
            miembros.add(fila);
        }

        // Lag = ultimo offset del topico - ultimo offset confirmado por el grupo
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(grupoId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        confirmados.keySet().forEach(tp -> pedido.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                pedido.isEmpty() ? Map.of() : esperar(admin.listOffsets(pedido).all());

        long lagTotal = 0;
        List<Map<String, Object>> offsets = new ArrayList<>();
        List<TopicPartition> ordenadas = new ArrayList<>(confirmados.keySet());
        ordenadas.sort(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition));
        for (TopicPartition tp : ordenadas) {
            long confirmado = confirmados.get(tp).offset();
            long fin = finales.get(tp).offset();
            long lag = Math.max(0, fin - confirmado);
            lagTotal += lag;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("topico", tp.topic());
            fila.put("particion", tp.partition());
            fila.put("offsetConfirmado", confirmado);
            fila.put("offsetFinal", fin);
            fila.put("lag", lag);
            offsets.add(fila);
        }

        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("grupo", grupoId);
        salida.put("estado", String.valueOf(descripcion.state()));
        salida.put("asignador", descripcion.partitionAssignor());
        salida.put("coordinador", descripcion.coordinator() != null ? descripcion.coordinator().id() : null);
        salida.put("totalMiembros", miembros.size());
        salida.put("lagTotal", lagTotal);
        salida.put("miembros", miembros);
        salida.put("offsets", offsets);
        return salida;
    }

    public void eliminarGrupo(String grupoId) {
        esperar(admin.deleteConsumerGroups(List.of(grupoId)).all());
    }

    // ==================================================================
    // MENSAJES
    // ==================================================================
    /**
     * Lee (sin confirmar offsets ni unirse a ningun grupo) los ultimos mensajes
     * de un topico. Sirve para inspeccionar el contenido de un DLT.
     */
    public List<Map<String, Object>> verMensajes(String topico, int cantidad, Integer particion) {
        validarNombre(topico);
        int limite = Math.max(1, Math.min(cantidad, 50));

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafkaadmin-peek");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

        try (KafkaConsumer<String, String> consumidor = new KafkaConsumer<>(props)) {
            List<PartitionInfo> info = consumidor.partitionsFor(topico, Duration.ofSeconds(10));
            if (info == null || info.isEmpty()) {
                throw new UnknownTopicOrPartitionException("El topico no existe: " + topico);
            }

            List<TopicPartition> particiones = new ArrayList<>();
            for (PartitionInfo pi : info) {
                if (particion == null || pi.partition() == particion) {
                    particiones.add(new TopicPartition(topico, pi.partition()));
                }
            }
            if (particiones.isEmpty()) {
                throw new IllegalArgumentException("El topico '" + topico + "' no tiene la particion " + particion);
            }

            consumidor.assign(particiones);
            Map<TopicPartition, Long> inicios = consumidor.beginningOffsets(particiones);
            Map<TopicPartition, Long> finales = consumidor.endOffsets(particiones);
            for (TopicPartition tp : particiones) {
                consumidor.seek(tp, Math.max(inicios.get(tp), finales.get(tp) - limite));
            }

            List<ConsumerRecord<String, String>> leidos = new ArrayList<>();
            long vence = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < vence) {
                ConsumerRecords<String, String> lote = consumidor.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : lote) {
                    leidos.add(r);
                }
                boolean completo = true;
                for (TopicPartition tp : particiones) {
                    if (consumidor.position(tp) < finales.get(tp)) {
                        completo = false;
                    }
                }
                if (completo) {
                    break;
                }
            }

            leidos.sort(Comparator.comparingLong((ConsumerRecord<String, String> r) -> r.timestamp()).reversed());
            List<Map<String, Object>> salida = new ArrayList<>();
            for (ConsumerRecord<String, String> r : leidos) {
                if (salida.size() >= limite) {
                    break;
                }
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("particion", r.partition());
                fila.put("offset", r.offset());
                fila.put("fecha", Instant.ofEpochMilli(r.timestamp()).toString());
                fila.put("clave", r.key());
                fila.put("valor", r.value());
                salida.add(fila);
            }
            return salida;
        }
    }

    public Map<String, Object> publicar(String topico, String clave, String valor) {
        validarNombre(topico);
        try {
            SendResult<String, String> resultado = publicador.send(topico, clave, valor)
                    .get(timeoutSegundos, TimeUnit.SECONDS);
            Map<String, Object> salida = new LinkedHashMap<>();
            salida.put("status", "publicado");
            salida.put("topico", topico);
            salida.put("particion", resultado.getRecordMetadata().partition());
            salida.put("offset", resultado.getRecordMetadata().offset());
            return salida;
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new KafkaNoDisponibleException("No se pudo publicar: " + causa.getMessage());
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondio en " + timeoutSegundos + "s al publicar");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Publicacion interrumpida");
        }
    }

    // ==================================================================
    // Utilidades
    // ==================================================================
    private void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches() || nombre.equals(".") || nombre.equals("..")) {
            throw new IllegalArgumentException("Nombre de topico invalido: " + nombre);
        }
        if (nombre.startsWith("__")) {
            throw new IllegalArgumentException("Los topicos internos de Kafka (__) no se pueden administrar desde aqui");
        }
    }

    /** Espera el resultado de una operacion de Kafka con tiempo maximo, devolviendo los errores de Kafka tal cual. */
    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutSegundos, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            if (causa instanceof RuntimeException re) {
                throw re;
            }
            throw new KafkaNoDisponibleException("Error de Kafka: " + causa.getMessage());
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondio en " + timeoutSegundos + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operacion interrumpida");
        }
    }
}
