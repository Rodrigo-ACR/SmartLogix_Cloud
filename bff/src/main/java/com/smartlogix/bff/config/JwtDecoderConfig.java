package com.smartlogix.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;

/**
 * JwtDecoder compuesto: acepta tokens de Azure AD (IDaaS) Y tokens locales.
 * 
 * Flujo:
 * 1. Intenta validar como token de Azure AD usando JWKS de Microsoft Entra ID
 * 2. Si falla (token local del sistema), valida con la clave secreta local
 * 
 * Esto permite que:
 * - Admins autenticados con MSAL (Azure AD) accedan al BFF ✅
 * - Clientes autenticados con login local también accedan ✅
 */
@Configuration
public class JwtDecoderConfig {

    // Clave local del sistema (misma que JwtUtil.java)
    private static final String LOCAL_SECRET = "12345678901234567890123456789012";

    // JWKS de Azure AD (Tenant Interno 01)
    private static final String AZURE_JWKS_URI = "https://login.microsoftonline.com/57bd7ed4-95cb-4813-b649-72cfc68924db/discovery/keys";

    @Bean
    public JwtDecoder jwtDecoder() {
        // Decoder para tokens de Azure AD
        NimbusJwtDecoder azureDecoder = NimbusJwtDecoder
                .withJwkSetUri(AZURE_JWKS_URI)
                .build();

        // Decoder para tokens locales (clave HMAC)
        SecretKeySpec secretKey = new SecretKeySpec(
                LOCAL_SECRET.getBytes(), "HMACSHA256");
        NimbusJwtDecoder localDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .build();

        // Decoder compuesto: intenta Azure AD primero, luego local
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