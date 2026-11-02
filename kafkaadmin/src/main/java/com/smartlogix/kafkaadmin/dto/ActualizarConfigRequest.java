package com.smartlogix.kafkaadmin.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/** Configuraciones del topico a modificar, ej: {"retention.ms":"86400000","cleanup.policy":"delete"}. */
public class ActualizarConfigRequest {

    @NotEmpty(message = "Debe indicar al menos una configuracion")
    private Map<String, String> configs;

    public Map<String, String> getConfigs() { return configs; }
    public void setConfigs(Map<String, String> configs) { this.configs = configs; }
}
