package com.smartlogix.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Nuevo total de particiones. Kafka solo permite AUMENTAR, nunca reducir. */
public class AumentarParticionesRequest {

    @Min(value = 1, message = "El total debe ser al menos 1")
    @Max(value = 100, message = "Maximo 100 particiones")
    private int total;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
