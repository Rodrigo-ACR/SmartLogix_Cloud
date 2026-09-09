package com.smartlogix.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JwtDecoder: acepta tokens del Tenant SmartLogix.
 * Admin y Cliente usan el mismo tenant — el rol viene en los claims del JWT.
 */
@Configuration
public class JwtDecoderConfig {

    // Tenant SmartLogix — Admin y Cliente
    private static final String JWKS_SMARTLOGIX =
        "https://login.microsoftonline.com/275bee47-23c3-4b55-87a5-37dc048751cb/discovery/keys";

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
            .withJwkSetUri(JWKS_SMARTLOGIX)
            .build();
    }
}