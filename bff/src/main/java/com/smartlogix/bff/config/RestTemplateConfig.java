package com.smartlogix.bff.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate CON @LoadBalanced: para Circuit Breaker y anotaciones
     * que requieren Eureka. Se inyecta por defecto donde se usa @Autowired.
     */
    @Bean
    @LoadBalanced
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * RestTemplate SIN @LoadBalanced: llama directamente por nombre de
     * contenedor Docker (inventario:8091, usuarios:8094, etc.).
     * Se inyecta con @Qualifier("plainRestTemplate").
     */
    @Bean("plainRestTemplate")
    public RestTemplate plainRestTemplate() {
        return new RestTemplate();
    }
}