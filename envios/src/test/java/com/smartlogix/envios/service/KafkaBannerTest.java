package com.smartlogix.envios.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaBannerTest {

    @Test
    void recuadro_debeIncluirTituloLineasYBordes() {
        String r = KafkaBanner.recuadro("Pedido 9002", "linea uno", "linea dos");
        assertTrue(r.contains("Pedido 9002"));
        assertTrue(r.contains("linea uno"));
        assertTrue(r.contains("linea dos"));
        assertTrue(r.contains("*".repeat(66)));
    }

    @Test
    void recuadro_debeOmitirLineasNulasOVacias() {
        String r = KafkaBanner.recuadro("Titulo", null, "  ", "visible");
        assertTrue(r.contains("visible"));
        assertFalse(r.contains("null"));
    }
}
