package com.pucpr.service;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import com.pucpr.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtService {

    private static final String DEFAULT_SECRET_KEY = "sua_chave_secreta_com_pelo_menos_32_caracteres_aqui";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(resolveSecret().getBytes(StandardCharsets.UTF_8));
    }

    private String resolveSecret() {
        String secret = System.getenv("JWT_SECRET");

        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET_KEY;
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 bytes.");
        }

        return secret;
    }

    /**
     * Gera o token assinado.
     * 1. Define o 'subject' (e-mail do usuário).
     * 2. Adiciona Claims customizadas (como o 'role').
     * 3. Define a data de emissão e expiração (ex: 15 min).
     * 4. Assina com a chave e o algoritmo HS256.
     */
    public String generateToken(Usuario user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getTipo())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000)) // 15 min
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrai o e-mail (subject) do token.
     * TODO: O ALUNO DEVE IMPLEMENTAR:
     * 1. Usar Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).
     * 2. Retornar o Subject do Payload.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    /**
     * Valida se o token é autêntico e não expirou.
     * TODO: O ALUNO DEVE IMPLEMENTAR:
     * 1. Tentar fazer o parse do token.
     * 2. Se o parse falhar (assinatura errada ou expirado), a biblioteca joga uma Exception.
     * 3. Retornar true se o token for válido e false caso capture uma exceção.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

}
