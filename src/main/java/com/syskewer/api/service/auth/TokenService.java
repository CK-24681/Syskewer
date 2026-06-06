package com.syskewer.api.service.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.syskewer.api.model.user.User;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration:172800}")
    private long expirationSeconds;

    /**
     * @param user usuário autenticado
     * @return JWT assinado com id, email e perfil
     */
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("syskewer-api")
                    .withSubject(user.getUsername())
                    .withClaim("id", user.getId())
                    .withClaim("email", user.getEmail())
                    .withClaim("role", user.getRole().getAuthority())
                    .withExpiresAt(Instant.now().plus(expirationSeconds, ChronoUnit.SECONDS))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    /**
     * @param token JWT do header Authorization
     * @return username ou string vazia se inválido/expirado
     */
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("syskewer-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            return "";
        }
    }
}
