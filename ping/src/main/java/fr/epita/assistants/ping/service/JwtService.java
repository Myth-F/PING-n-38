package fr.epita.assistants.ping.service;

import fr.epita.assistants.ping.data.model.UserModel;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;

@ApplicationScoped
public class JwtService {

    public String generateToken(UserModel user) {
        Instant now = Instant.now();
        Instant exp = now.plus(24, ChronoUnit.HOURS);

        String role = user.getIsAdmin() ? "admin" : "user";

        return Jwt.issuer("ping-api")
                .subject(user.getId().toString())
                .groups(new HashSet<>(Arrays.asList(role)))
                .issuedAt(now)
                .expiresAt(exp)
                .sign();
    }
}