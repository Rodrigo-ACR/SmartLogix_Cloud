package com.smartlogix.inventario.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro de eventos Kafka ya procesados (idempotencia). Kafka garantiza
 * "al menos una vez": ante un reinicio puede entregar el mismo evento dos
 * veces. Antes de modificar el stock se consulta esta tabla por eventId.
 */
@Entity
@Table(name = "eventos_procesados")
public class EventoProcesado {

    @Id
    private String eventId;

    private String tipo;

    private Instant procesadoEn;

    protected EventoProcesado() {
    }

    public EventoProcesado(String eventId, String tipo) {
        this.eventId = eventId;
        this.tipo = tipo;
        this.procesadoEn = Instant.now();
    }

    public String getEventId() { return eventId; }

    public String getTipo() { return tipo; }

    public Instant getProcesadoEn() { return procesadoEn; }
}
