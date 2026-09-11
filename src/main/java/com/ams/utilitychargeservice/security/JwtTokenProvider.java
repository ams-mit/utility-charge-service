package com.ams.utilitychargeservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.Base64;

@Slf4j
@Component
public class JwtTokenProvider {

    private final PublicKey gatewayPublicKey;
    private final PrivateKey servicePrivateKey;

    public JwtTokenProvider(
            @Value("${ams.security.gateway-public-key}") String gatewayPublicKeyStr,
            @Value("${ams.security.service-private-key}") String servicePrivateKeyStr) {
        this.gatewayPublicKey = parsePublicKey(gatewayPublicKeyStr);
        this.servicePrivateKey = parsePrivateKey(servicePrivateKeyStr);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired");
        } catch (Exception e) {
            log.warn("JWT token invalid: {}", e.getMessage());
        }
        return false;
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractType(String token) {
        return (String) parseClaims(token).get("type");
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?>) {
            return (List<String>) roles;
        }
        return List.of();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(gatewayPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PublicKey parsePublicKey(String keyStr) {
        try {
            log.info("Parsing Gateway Public Key. Input length: {}", keyStr == null ? 0 : keyStr.length());
            String cleanKey = keyStr
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            log.info("Cleaned key length: {}", cleanKey.length());
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            log.error("Error parsing Gateway Public Key: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Gateway Public Key", e);
        }
    }

    private PrivateKey parsePrivateKey(String keyStr) {
        try {
            String cleanKey = keyStr
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Service Private Key", e);
        }
    }
}
