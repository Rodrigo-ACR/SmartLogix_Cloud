package com.smartlogix.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;

/**
 * JwtDecoder compuesto: acepta tokens de Azure AD (Tenant SmartLogix) Y
 * tokens locales emitidos por el propio BFF para clientes registrados
 * con correo/contraseña.
 */
@Configuration
public class JwtDecoderConfig {

    // Clave local del sistema (misma que usa el AuthService para firmar tokens locales)
    private static final String LOCAL_SECRET = "12345678901234567890123456789012";

    // JWKS del Tenant SmartLogix
    private static final String AZURE_JWKS_URI =
        "https://login.microsoftonline.com/275bee47-23c3-4b55-87a5-37dc048751cb/discovery/keys";

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder azureDecoder = NimbusJwtDecoder
            .withJwkSetUri(AZURE_JWKS_URI)
            .build();

        SecretKeySpec secretKey = new SecretKeySpec(
            LOCAL_SECRET.getBytes(), "HMACSHA256");
        NimbusJwtDecoder localDecoder = NimbusJwtDecoder
            .withSecretKey(secretKey)
            .build();

        return token -> {
            try {
                return azureDecoder.decode(token);
            } catch (JwtException e) {
                try {
                    return localDecoder.decode(token);
                } catch (JwtException ex) {
                    throw new JwtException("Token inválido: no es ni Azure AD ni token local. " + ex.getMessage());
                }
            }
        };
    }
}