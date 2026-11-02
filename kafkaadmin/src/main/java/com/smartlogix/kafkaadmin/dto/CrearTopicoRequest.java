package com.smartlogix.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO para crear un topico. Si no se indican particiones/replicas se usan los
 * valores del sistema (3 y 3). "configs" acepta cualquier configuracion de
 * topico de Kafka, por ejemplo: retention.ms, cleanup.policy, min.insync.replicas.
 */
public class CrearTopicoRequest {

    @NotBlank(message = "El nombre del topico no puede estar vacio")
    @Pattern(regexp = "^[a-zA-Z0-9._\\-]{1,200}$",
            message = "El nombre solo puede contener letras, numeros, puntos, guiones y guiones bajos (max 200)")
    private String nombre;

    @Min(value = 1, message = "Debe tener al menos 1 particion")
    @Max(value = 100, message = "Maximo 100 particiones")
    private int particiones = 3;

    @Min(value = 1, message = "Debe tener al menos 1 replica")
    @Max(value = 10, message = "Maximo 10 replicas")
    private int replicas = 3;

    private Map<String, String> configs = new HashMap<>();

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getParticiones() { return particiones; }
    public void setParticiones(int particiones) { this.particiones = particiones; }

    public int getReplicas() { return replicas; }
    public void setReplicas(int replicas) { this.replicas = replicas; }

    public Map<String, String> getConfigs() { return configs; }
    public void setConfigs(Map<String, String> configs) { this.configs = configs; }
}
