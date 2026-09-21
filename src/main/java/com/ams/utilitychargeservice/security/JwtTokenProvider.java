package com.ams.utilitychargeservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.annotation.PostConstruct;
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

    @Value("${ams.security.gateway-public-key}")
    private String gatewayPublicKeyStr;

    @Value("${ams.security.service-private-key}")
    private String servicePrivateKeyStr;

    private PublicKey gatewayPublicKey;
    private PrivateKey servicePrivateKey;
    private PublicKey servicePublicKey; // Added for dev-mode token validation

    public PublicKey getGatewayPublicKey() {
        return gatewayPublicKey;
    }

    public PrivateKey getServicePrivateKey() {
        return servicePrivateKey;
    }

    @PostConstruct
    public void init() {
        this.gatewayPublicKey = parsePublicKey(gatewayPublicKeyStr);
        this.servicePrivateKey = parsePrivateKey(servicePrivateKeyStr);
        this.servicePublicKey = derivePublicKey(servicePrivateKey);
    }

    private PublicKey derivePublicKey(PrivateKey privateKey) {
        try {
            // For RSA, we can't simply "cast" but we can use the key factory
            // or just parse the public key from an env var if available.
            // Since we have the private key, we can extract the public modulus and exponent.
            java.security.interfaces.RSAPrivateKey rsaPriv = (java.security.interfaces.RSAPrivateKey) privateKey;
            java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(
                    rsaPriv.getModulus(),
                    java.math.BigInteger.valueOf(65537) // Standard RSA exponent
            );
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            log.error("Failed to derive service public key: {}", e.getMessage());
            return null;
        }
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
        try {
            return Jwts.parser()
                    .verifyWith(gatewayPublicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            // Fallback for DevTokenController: try verifying with service public key
            if (servicePublicKey != null) {
                try {
                    return Jwts.parser()
                            .verifyWith(servicePublicKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                } catch (Exception ex) {
                    log.debug("Token failed both gateway and service key verification");
                }
            }
            throw e;
        }
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
    public String generateServiceJwt() {
        return Jwts.builder()
                .subject("utility-charge-service")
                .claim("type", "service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 5L * 60 * 1000)) // 5 minutes
                .signWith(servicePrivateKey, Jwts.SIG.RS256)
                .compact();
    }

}
