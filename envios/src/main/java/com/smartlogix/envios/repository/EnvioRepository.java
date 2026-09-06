package com.smartlogix.envios.repository;

import com.smartlogix.envios.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
}