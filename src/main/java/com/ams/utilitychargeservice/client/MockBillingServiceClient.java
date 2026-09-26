package com.ams.utilitychargeservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock implementation of BillingServiceClient for local development.
 *
 * Active on the 'dev' Spring profile only.
 * Always returns false — meaning "no invoice exists" —
 * so you can freely test update and delete without needing billing-payment-service running.
 *
 * USAGE: In Postman you can override this behaviour by switching to
 * the prod profile and pointing API_GATEWAY_URL at a real billing service.
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class MockBillingServiceClient implements BillingServiceClient {

    @Override
    public boolean invoiceExistsForPeriod(UUID unitId, int billingYear, int billingMonth) {
        log.warn("[MOCK] BillingServiceClient.invoiceExistsForPeriod called — returning false (dev mode). " +
                "unitId={}, year={}, month={}", unitId, billingYear, billingMonth);
        return false;
    }
}