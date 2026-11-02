package com.smartlogix.inventario.service;

/**
 * Arma el recuadro con asteriscos que se imprime en los logs de Kafka
 * (evento recibido, publicado, reintento o enviado a DLT), para que sea facil
 * de ubicar en la salida de "docker logs".
 */
public final class KafkaBanner {

    private static final String LINEA = "*".repeat(66);

    private KafkaBanner() {}

    public static String recuadro(String titulo, String... lineas) {
        StringBuilder sb = new StringBuilder("\n").append(LINEA).append('\n');
        sb.append(' ').append(titulo).append('\n');
        for (String l : lineas) {
            if (l != null && !l.isBlank()) {
                sb.append(' ').append(l).append('\n');
            }
        }
        return sb.append(LINEA).toString();
    }
}
