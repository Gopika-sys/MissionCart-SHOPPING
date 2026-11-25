package com.missioncart.security;

import com.missioncart.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(AppProperties props) {
        this.accessKey = Keys.hmacShaKeyFor(padSecret(props.jwt().secret()).getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(padSecret(props.jwt().refreshSecret()).getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = props.jwt().accessExpirationMs();
        this.refreshExpirationMs = props.jwt().refreshExpirationMs();
    }

    private String padSecret(String raw) {
        if (raw == null || raw.length() < 32) {
            return (raw == null ? "" : raw) + "_missioncart_default_hmac_padding_32bytes_min";
        }
        return raw;
    }

    public String generateAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
            .subject(email)
            .claims(Map.of("uid", userId, "role", role, "type", "access"))
            .issuedAt(now)
            .expiration(new Date(now.getTime() + accessExpirationMs))
            .signWith(accessKey)
            .compact();
    }

    public String generateRefreshToken(Long userId, String email, String role) {
        Date now = new Date();
        return Jwts.builder()
            .subject(email)
            .claims(Map.of("uid", userId, "role", role, "type", "refresh"))
            .issuedAt(now)
            .expiration(new Date(now.getTime() + refreshExpirationMs))
            .signWith(refreshKey)
            .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token).getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token).getPayload();
    }
}
