package com.ams.utilitychargeservice.client;

import java.util.UUID;

/**
 * Contract for checking whether billing-payment-service has already generated
 * an invoice for a given unit and billing period.
 *
 * WHY THIS EXISTS:
 * From API Reference v2.1, Section 3.3 (Business Rules):
 * "A utility charge record can only be updated or deleted if no invoice has been
 * generated for that unit and period. Returns 409 if invoice already exists."
 *
 * This service must call billing-payment-service to check.
 * All inter-service calls route through the API Gateway (never direct URLs).
 *
 * TWO IMPLEMENTATIONS:
 * - MockBillingServiceClient  → active on 'dev' profile  → always returns false (no invoice)
 * - RealBillingServiceClient  → active on 'prod' profile → real HTTP call to gateway
 */
public interface BillingServiceClient {

    /**
     * Returns true if billing-payment-service has an invoice for the given unit and period.
     * If the billing service is unreachable, implementations should throw a runtime exception
     * so the caller (service layer) can handle it as a 503.
     *
     * @param unitId       the unit UUID
     * @param billingYear  the billing year
     * @param billingMonth the billing month (1–12)
     * @return true if an invoice exists, false otherwise
     */
    boolean invoiceExistsForPeriod(UUID unitId, int billingYear, int billingMonth);
}