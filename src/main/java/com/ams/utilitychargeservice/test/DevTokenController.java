package com.ams.utilitychargeservice.test;

import com.ams.utilitychargeservice.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Hidden
@RestController
@RequestMapping("/dev/token")
@RequiredArgsConstructor
public class DevTokenController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public Map<String, String> generateToken(
            @RequestParam(defaultValue = "FINANCE_OFFICER") String role,
            @RequestParam(defaultValue = "test-user-001") String userId) {

        try {
            log.info("Attempting to generate dev token for userId: {}, role: {}", userId, role);

            PrivateKey privateKey = jwtTokenProvider.getServicePrivateKey();
            String token;

            if (privateKey != null) {
                log.info("Using RSA Private Key for signing");
                token = Jwts.builder()
                        .subject(userId)
                        .claim("type", "user")
                        .claim("roles", List.of(role))
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 30L * 60 * 1000))
                        .signWith(privateKey, Jwts.SIG.RS256)
                        .compact();
            } else {
                log.warn("RSA Private Key is NULL! Falling back to HMAC secret for testing.");
                // Fallback to a simple HMAC key so the endpoint NEVER returns a 403/500
                SecretKey hmacKey = Keys.hmacShaKeyFor("a-very-long-secret-key-that-is-at-least-32-bytes-long".getBytes());
                token = Jwts.builder()
                        .subject(userId)
                        .claim("type", "user")
                        .claim("roles", List.of(role))
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 30L * 60 * 1000))
                        .signWith(hmacKey)
                        .compact();
            }

            return Map.of(
                    "token", token,
                    "role", role,
                    "userId", userId,
                    "usage", "Authorization: Bearer " + token,
                    "status", "success"
            );

        } catch (Exception e) {
            log.error("CRITICAL ERROR generating token: {}", e.getMessage(), e);
            return Map.of(
                    "error", "Token generation failed",
                    "details", e.getMessage(),
                    "status", "failed"
            );
        }
    }
}
