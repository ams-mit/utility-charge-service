package com.ams.utilitychargeservice.test;

import com.ams.utilitychargeservice.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DEV ONLY — Generates test Gateway JWTs for Postman testing.
 * Only active on the 'dev' profile. NEVER deploy to production.
 *
 * This simulates what the API Gateway does: takes a user identity and roles,
 * signs a Gateway JWT with the gateway private key, which your service then
 * validates using the gateway public key from your .env file.
 */
@Hidden
@RestController
@RequestMapping("/dev/token")
@Profile("dev")
@RequiredArgsConstructor
public class DevTokenController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Generates a Gateway User JWT for Postman testing.
     *
     * Usage:
     * GET http://localhost:8082/dev/token?role=FINANCE_OFFICER&userId=test-user-001
     *
     * Supported roles: FINANCE_OFFICER, APARTMENT_MANAGER, RESIDENT, TENANT, OWNER
     */
    @GetMapping
    public Map<String, String> generateToken(
            @RequestParam(defaultValue = "FINANCE_OFFICER") String role,
            @RequestParam(defaultValue = "test-user-001") String userId) throws Exception {

        // We cannot use the public key to SIGN a token.
        // Signing requires a Private Key.
        // For dev purposes, we use the service's own private key to simulate a signed token.
        // Note: In production, only the Gateway signs these tokens.

        PrivateKey privateKey = jwtTokenProvider.getServicePrivateKey();

        String token = Jwts.builder()
                .subject(userId)
                .claim("type", "user")
                .claim("roles", List.of(role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 30L * 60 * 1000)) // 30 min
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        return Map.of(
                "token", token,
                "role", role,
                "userId", userId,
                "usage", "Authorization: Bearer " + token
        );
    }
}