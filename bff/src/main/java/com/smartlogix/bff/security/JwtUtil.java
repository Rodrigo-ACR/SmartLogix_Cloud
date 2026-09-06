package com.smartlogix.bff.security;

import com.smartlogix.bff.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 🔥 CLAVE FIJA (32+ caracteres)
    private final String SECRET = "12345678901234567890123456789012";

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
    

    public String generarToken(Usuario user) {

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("rol", user.getRol())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 horas
                .signWith(key)
                .compact();
    }
}