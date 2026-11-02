package com.smartlogix.kafkaadmin.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecursosSistemaTest {

    @Test
    void topicosDeNegocioYDlt_estanProtegidos() {
        assertTrue(RecursosSistema.topicoProtegido("smartlogix.pedidos.eventos"));
        assertTrue(RecursosSistema.topicoProtegido("smartlogix.logistica.eventos"));
        assertTrue(RecursosSistema.topicoProtegido("smartlogix.pedidos.eventos.DLT"));
        assertTrue(RecursosSistema.topicoProtegido("smartlogix.logistica.eventos.DLT"));
    }

    @Test
    void topicoAjeno_noEstaProtegido() {
        assertFalse(RecursosSistema.topicoProtegido("mi.topico.de.prueba"));
    }
}
