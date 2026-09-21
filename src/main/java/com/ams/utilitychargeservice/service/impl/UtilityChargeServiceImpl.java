package com.ams.utilitychargeservice.service.impl;

import com.ams.utilitychargeservice.client.BillingServiceClient;
import com.ams.utilitychargeservice.dto.request.UtilityChargeRequest;
import com.ams.utilitychargeservice.dto.request.UtilityChargeUpdateRequest;
import com.ams.utilitychargeservice.dto.response.InternalUtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeSummaryResponse;
import com.ams.utilitychargeservice.entity.UtilityCharge;
import com.ams.utilitychargeservice.entity.UtilityRate;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.exception.DuplicateResourceException;
import com.ams.utilitychargeservice.exception.InvoiceAlreadyExistsException;
import com.ams.utilitychargeservice.exception.ResourceNotFoundException;
import com.ams.utilitychargeservice.mapper.UtilityChargeMapper;
import com.ams.utilitychargeservice.repository.UtilityChargeRepository;
import com.ams.utilitychargeservice.repository.UtilityRateRepository;
import com.ams.utilitychargeservice.service.UtilityChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Business logic for Utility Charge management.
 *
 * CRITICAL DESIGN RULE — Rate Snapshot (from API Reference v2.1 Section 6.1 and architecture-decisions):
 * When recording a utility charge, the service fetches the ACTIVE rate for the utility type
 * and copies ratePerUnit into ratePerUnitSnapshot on the UtilityCharge entity.
 * This means updating a UtilityRate later does NOT change any previously recorded charge.
 * Each charge permanently records the rate that was active at the time it was created.
 *
 * CRITICAL BUSINESS RULE — Invoice Protection (API Reference v2.1 Section 3.3):
 * A utility charge can only be updated (UTIL-009) or deleted (UTIL-010) if no invoice
 * has been generated for that unit and period in billing-payment-service.
 * If an invoice exists, the service returns 409 Conflict.
 * This check is done via BillingServiceClient (mock on dev, real on prod).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityChargeServiceImpl implements UtilityChargeService {

    private final UtilityChargeRepository utilityChargeRepository;
    private final UtilityRateRepository utilityRateRepository;
    private final UtilityChargeMapper utilityChargeMapper;
    private final BillingServiceClient billingServiceClient;

    // ─────────────────────────────────────────────────────────────────
    // UTIL-006 — POST /api/v1/utility-charges
    // Records a utility charge for a unit and billing period.
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UtilityChargeResponse recordCharge(UtilityChargeRequest request) {
        log.info("Recording utility charge: unitId={}, type={}, year={}, month={}",
                request.getUnitId(), request.getUtilityType(),
                request.getBillingYear(), request.getBillingMonth());

        /*
         * DUPLICATE CHECK — warn before overwriting.
         * Per API Reference v2.1 Section 6.2 Note:
         * "Warns if a record already exists for same unit + type + period."
         *
         * We throw DuplicateResourceException here. The Finance Officer
         * must explicitly use UTIL-009 (PUT) to overwrite an existing charge.
         * This prevents accidental double-entries from two form submissions.
         */
        Optional<UtilityCharge> existing = utilityChargeRepository
                .findByUnitIdAndUtilityTypeAndBillingYearAndBillingMonth(
                        request.getUnitId(),
                        request.getUtilityType(),
                        request.getBillingYear(),
                        request.getBillingMonth());

        if (existing.isPresent()) {
            throw new DuplicateResourceException(
                    "A utility charge already exists for unit " + request.getUnitId() +
                            ", type=" + request.getUtilityType() +
                            ", period=" + request.getBillingYear() + "-" + request.getBillingMonth() +
                            ". Use PUT /api/v1/utility-charges/" + existing.get().getId() +
                            " to correct it (only allowed before invoice generation)."
            );
        }

        /*
         * FETCH ACTIVE RATE — required before calculating the charge.
         * If no ACTIVE rate exists for this utility type, we cannot proceed.
         * The Finance Officer must create a rate (UTIL-001) first.
         */
        UtilityRate activeRate = utilityRateRepository
                .findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(
                        request.getUtilityType(), RateStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active Utility Rate for " + request.getUtilityType(),
                        "No ACTIVE rate found. Create one via POST /api/v1/utility-rates first."
                ));

        /*
         * CALCULATE AMOUNT — snapshot the rate at this exact moment.
         * calculatedAmount = usageValue × ratePerUnit
         * Rounded to 2 decimal places using HALF_UP (standard accounting rounding).
         *
         * CRITICAL: we store activeRate.getRatePerUnit() as ratePerUnitSnapshot.
         * If someone updates the rate tomorrow, this charge still shows today's rate.
         */
        BigDecimal calculatedAmount = request.getUsageValue()
                .multiply(activeRate.getRatePerUnit())
                .setScale(2, RoundingMode.HALF_UP);

        UtilityCharge charge = UtilityCharge.builder()
                .unitId(request.getUnitId())
                .utilityType(request.getUtilityType())
                .billingYear(request.getBillingYear())
                .billingMonth(request.getBillingMonth())
                .usageValue(request.getUsageValue())
                .ratePerUnitSnapshot(activeRate.getRatePerUnit())  // SNAPSHOT — never a FK reference
                .utilityRateId(activeRate.getId())                 // stored for audit/traceability only
                .calculatedAmount(calculatedAmount)
                .build();

        UtilityCharge saved = utilityChargeRepository.save(charge);
        log.info("Utility charge recorded: id={}, amount={}", saved.getId(), saved.getCalculatedAmount());

        return utilityChargeMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-007 — GET /api/v1/utility-charges
    // Lists all utility charges with optional filters, paginated.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public Page<UtilityChargeResponse> getAllCharges(UUID unitId, UtilityType utilityType,
                                                     Integer year, Integer month, Pageable pageable) {
        log.debug("Fetching utility charges: unitId={}, type={}, year={}, month={}",
                unitId, utilityType, year, month);

        /*
         * Dynamic filtering: JPA Specifications would be cleaner for many filter combinations,
         * but for 4 optional params the if-else approach is readable and sufficient here.
         * All combinations are handled explicitly.
         */
        Page<UtilityCharge> page;

        if (unitId != null && utilityType != null && year != null && month != null) {
            page = utilityChargeRepository
                    .findByUnitIdAndUtilityTypeAndBillingYearAndBillingMonth(
                            unitId, utilityType, year, month, pageable);
        } else if (unitId != null && year != null && month != null) {
            page = utilityChargeRepository
                    .findByUnitIdAndBillingYearAndBillingMonthPageable(unitId, year, month, pageable);
        } else if (unitId != null) {
            page = utilityChargeRepository.findByUnitId(unitId, pageable);
        } else if (utilityType != null) {
            page = utilityChargeRepository.findByUtilityType(utilityType, pageable);
        } else {
            page = utilityChargeRepository.findAll(pageable);
        }

        return page.map(utilityChargeMapper::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-008 — GET /api/v1/utility-charges/{utilityChargeId}
    // Returns a single utility charge by its UUID.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public UtilityChargeResponse getChargeById(UUID utilityChargeId) {
        log.debug("Fetching utility charge by id: {}", utilityChargeId);
        return utilityChargeMapper.toResponse(findChargeOrThrow(utilityChargeId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-009 — PUT /api/v1/utility-charges/{utilityChargeId}
    // Corrects a utility charge — only allowed before invoice generation.
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UtilityChargeResponse updateCharge(UUID utilityChargeId, UtilityChargeUpdateRequest request) {
        log.info("Updating utility charge: id={}", utilityChargeId);

        UtilityCharge charge = findChargeOrThrow(utilityChargeId);

        /*
         * INVOICE PROTECTION CHECK (API Reference v2.1 Section 3.3):
         * If billing-payment-service has already generated an invoice for this
         * unit and period, we must NOT allow the charge to be changed.
         * The invoice already snapshotted this charge's amount as a line item.
         * Changing it here would make the charge and invoice inconsistent.
         */
        checkNoInvoiceExists(charge.getUnitId(), charge.getBillingYear(), charge.getBillingMonth());

        /*
         * RECALCULATE WITH CURRENT ACTIVE RATE.
         * When correcting a usage value, we use the currently ACTIVE rate
         * for the utility type — NOT the original rate that was snapshotted.
         *
         * WHY? The correction could be happening days or weeks later.
         * Using the current active rate is consistent with "this rate is what
         * we charge right now". The Finance Officer is responsible for ensuring
         * the rate is correct before making corrections.
         */
        UtilityRate activeRate = utilityRateRepository
                .findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(
                        charge.getUtilityType(), RateStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active Utility Rate for " + charge.getUtilityType(),
                        "Cannot update charge — no ACTIVE rate found for this utility type."
                ));

        BigDecimal recalculatedAmount = request.getUsageValue()
                .multiply(activeRate.getRatePerUnit())
                .setScale(2, RoundingMode.HALF_UP);

        charge.setUsageValue(request.getUsageValue());
        charge.setRatePerUnitSnapshot(activeRate.getRatePerUnit());
        charge.setCalculatedAmount(recalculatedAmount);
        charge.setUtilityRateId(activeRate.getId());

        UtilityCharge updated = utilityChargeRepository.save(charge);
        log.info("Utility charge corrected: id={}, newAmount={}", updated.getId(), updated.getCalculatedAmount());

        return utilityChargeMapper.toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-010 — DELETE /api/v1/utility-charges/{utilityChargeId}
    // Removes a utility charge — only allowed before invoice generation.
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteCharge(UUID utilityChargeId) {
        log.info("Deleting utility charge: id={}", utilityChargeId);

        UtilityCharge charge = findChargeOrThrow(utilityChargeId);

        /*
         * Same invoice protection check as UTIL-009.
         * Cannot delete a charge that has already been included in an invoice.
         */
        checkNoInvoiceExists(charge.getUnitId(), charge.getBillingYear(), charge.getBillingMonth());

        utilityChargeRepository.delete(charge);
        log.info("Utility charge deleted: id={}", utilityChargeId);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-011 — GET /api/v1/utility-charges/units/{unitId}
    // Full utility charge history for a unit across all periods and types.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public List<UtilityChargeResponse> getChargesForUnit(UUID unitId) {
        log.debug("Fetching all utility charges for unit: {}", unitId);
        List<UtilityCharge> charges = utilityChargeRepository.findByUnitId(unitId);
        return utilityChargeMapper.toResponseList(charges);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-012 — GET /api/v1/utility-charges/units/{unitId}/period/{year}/{month}
    // All utility charges for a unit in a specific billing period.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public List<UtilityChargeResponse> getChargesForUnitAndPeriod(UUID unitId, Integer year, Integer month) {
        log.debug("Fetching utility charges for unit={}, period={}-{}", unitId, year, month);
        List<UtilityCharge> charges = utilityChargeRepository
                .findByUnitIdAndBillingYearAndBillingMonth(unitId, year, month);
        return utilityChargeMapper.toResponseList(charges);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-013 — GET /api/v1/utility-charges/summary
    // Aggregate totals per utility type and grand total for a period.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public UtilityChargeSummaryResponse getSummary(Integer year, Integer month) {
        log.debug("Generating utility charge summary for period={}-{}", year, month);

        /*
         * Fetch all charges across all units for this period.
         * Then aggregate in Java — for a student project this is
         * simpler and more readable than a native JPQL GROUP BY query.
         * For production scale, this would move to a @Query in the repository.
         */
        List<UtilityCharge> allChargesForPeriod = utilityChargeRepository
                .findByBillingYearAndBillingMonth(year, month);

        // Group and sum by utility type
        Map<UtilityType, BigDecimal> totalsByType = new EnumMap<>(UtilityType.class);
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (UtilityCharge charge : allChargesForPeriod) {
            totalsByType.merge(charge.getUtilityType(),
                    charge.getCalculatedAmount(), BigDecimal::add);
            grandTotal = grandTotal.add(charge.getCalculatedAmount());
        }

        return UtilityChargeSummaryResponse.builder()
                .billingYear(year)
                .billingMonth(month)
                .totalsByType(totalsByType)
                .grandTotal(grandTotal)
                .recordCount((long) allChargesForPeriod.size())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-014 (Part 3) — Called by InternalUtilityController
    // Returns utility charges for billing-payment-service to use
    // as invoice line items. Returns empty list (NEVER 404) if none exist.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public List<InternalUtilityChargeResponse> getChargesForInvoiceGeneration(
            UUID unitId, Integer year, Integer month) {
        log.info("[INTERNAL] Fetching utility charges for invoice generation: unitId={}, {}-{}",
                unitId, year, month);

        /*
         * CRITICAL FROM API REFERENCE v2.1 SECTION 6.3:
         * "Returns empty array [] (not 404) if no utility charges exist for the requested period."
         * "billing-payment-service must handle an empty array gracefully during invoice generation."
         *
         * If we returned 404, billing-payment-service's invoice generation would fail
         * whenever a unit has no utility charges — which is a valid scenario
         * (e.g. a storage unit with no electricity).
         * An empty list means "no utility charges this period" — perfectly fine.
         */
        List<UtilityCharge> charges = utilityChargeRepository
                .findByUnitIdAndBillingYearAndBillingMonth(unitId, year, month);

        if (charges.isEmpty()) {
            log.info("[INTERNAL] No utility charges found for unitId={}, {}-{} — returning empty list",
                    unitId, year, month);
        }

        return utilityChargeMapper.toInternalResponseList(charges);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    private UtilityCharge findChargeOrThrow(UUID utilityChargeId) {
        return utilityChargeRepository.findById(utilityChargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility Charge", utilityChargeId));
    }

    /**
     * Checks whether billing-payment-service has an invoice for the unit+period.
     * Throws InvoiceAlreadyExistsException (409) if one exists.
     * Used by UTIL-009 (update) and UTIL-010 (delete) to enforce the business rule.
     */
    private void checkNoInvoiceExists(UUID unitId, int billingYear, int billingMonth) {
        boolean invoiceExists = billingServiceClient
                .invoiceExistsForPeriod(unitId, billingYear, billingMonth);

        if (invoiceExists) {
            throw new InvoiceAlreadyExistsException(unitId.toString(), billingYear, billingMonth);
        }
    }
}