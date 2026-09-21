package com.ams.utilitychargeservice.client;

import com.ams.utilitychargeservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Real implementation of BillingServiceClient for production.
 *
 * Active on the 'prod' Spring profile only.
 *
 * Makes an HTTP GET to the API Gateway which routes to billing-payment-service.
 * Uses a Service JWT signed with this service's own private key (per v2.1 Section 2.7).
 *
 * HOW THE CALL WORKS (API Reference v2.1, Section 2.7):
 * 1. This service creates a Service JWT signed with its own private key.
 * 2. Sends it to API_GATEWAY_URL/api/v1/internal/invoices/exists?unitId=...&year=...&month=...
 * 3. Gateway verifies the Service JWT, creates a new Gateway Service JWT, forwards it.
 * 4. billing-payment-service receives Gateway JWT, checks type=service, returns result.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RealBillingServiceClient implements BillingServiceClient {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${ams.service.api-gateway-url}")
    private String apiGatewayUrl;

    @Override
    public boolean invoiceExistsForPeriod(UUID unitId, int billingYear, int billingMonth) {
        String url = apiGatewayUrl +
                "/api/v1/internal/invoices/exists?unitId=" + unitId +
                "&year=" + billingYear +
                "&month=" + billingMonth;

        log.info("[REAL] Checking invoice existence via Gateway: unitId={}, {}-{}", unitId, billingYear, billingMonth);

        try {
            // Create a Service JWT signed with this service's private key
            String serviceJwt = jwtTokenProvider.generateServiceJwt();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + serviceJwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
                Object exists = data.get("invoiceExists");
                return Boolean.TRUE.equals(exists);
            }
            return false;

        } catch (HttpClientErrorException.NotFound e) {
            // 404 from billing service means no invoice exists
            return false;
        } catch (ResourceAccessException e) {
            log.error("Cannot reach billing-payment-service via Gateway: {}", e.getMessage());
            throw new RuntimeException("billing-payment-service is unavailable — cannot verify invoice status", e);
        } catch (Exception e) {
            log.error("Unexpected error checking invoice existence: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to check invoice existence", e);
        }
    }
}