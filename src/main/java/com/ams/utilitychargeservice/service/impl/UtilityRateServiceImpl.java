package com.ams.utilitychargeservice.service.impl;

import com.ams.utilitychargeservice.dto.request.UtilityRateRequest;
import com.ams.utilitychargeservice.dto.request.UtilityRateStatusRequest;
import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.entity.UtilityRate;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.exception.DuplicateResourceException;
import com.ams.utilitychargeservice.exception.ResourceNotFoundException;
import com.ams.utilitychargeservice.mapper.UtilityRateMapper;
import com.ams.utilitychargeservice.repository.UtilityRateRepository;
import com.ams.utilitychargeservice.service.UtilityRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for Utility Rate management.
 *
 * KEY RULE FROM API REFERENCE v2.1 (Section 6.1):
 * "Updating a rate does not affect already-recorded charges."
 * This is enforced by the ratePerUnitSnapshot field on UtilityCharge —
 * each charge record copies the rate at the time it was recorded.
 * This service does NOT need to worry about existing charges when updating a rate.
 *
 * KEY RULE — One ACTIVE rate per type:
 * You cannot have two ACTIVE rates for WATER at the same time.
 * The system must have exactly one active rate per utility type for
 * the charge calculation to be unambiguous.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // All methods are read-only by default. Write methods override this.
public class UtilityRateServiceImpl implements UtilityRateService {

    private final UtilityRateRepository utilityRateRepository;
    private final UtilityRateMapper utilityRateMapper;

    // ─────────────────────────────────────────────────────────────────
    // UTIL-001 — POST /api/v1/utility-rates
    // Creates a new utility rate for a given utility type.
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional  // This method writes to the database, so we override readOnly
    public UtilityRateResponse createRate(UtilityRateRequest request) {
        log.info("Creating utility rate: type={}, ratePerUnit={}", request.getUtilityType(), request.getRatePerUnit());

        /*
         * BUSINESS RULE: Only one ACTIVE rate per utility type at a time.
         *
         * WHY? When Part 2 records a utility charge, it calls
         * findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(type, ACTIVE)
         * to get the current rate. If there were two ACTIVE WATER rates,
         * the result would be ambiguous.
         *
         * If a Finance Officer wants to change the WATER rate, they must
         * first DEACTIVATE the old one, then create the new one.
         * OR they can simply UPDATE the existing active rate (UTIL-004).
         */
        boolean activeRateExists = utilityRateRepository
                .findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(
                        request.getUtilityType(), RateStatus.ACTIVE)
                .isPresent();

        if (activeRateExists) {
            throw new DuplicateResourceException(
                    "An ACTIVE rate already exists for utility type: " + request.getUtilityType() +
                            ". Deactivate the existing rate first (UTIL-005), or update it directly (UTIL-004)."
            );
        }

        // Build the entity from the request DTO
        UtilityRate rate = UtilityRate.builder()
                .utilityType(request.getUtilityType())
                .ratePerUnit(request.getRatePerUnit())
                .unitDescription(request.getUnitDescription())
                .effectiveFrom(request.getEffectiveFrom())
                .status(RateStatus.ACTIVE)  // New rates are always ACTIVE
                .build();

        UtilityRate saved = utilityRateRepository.save(rate);
        log.info("Utility rate created: id={}, type={}", saved.getId(), saved.getUtilityType());

        return utilityRateMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-002 — GET /api/v1/utility-rates
    // Lists all utility rates with optional filters.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public List<UtilityRateResponse> getAllRates(UtilityType utilityType, RateStatus status) {
        log.debug("Fetching utility rates: utilityType={}, status={}", utilityType, status);

        List<UtilityRate> rates;

        /*
         * FILTERING LOGIC:
         * The caller can filter by utilityType, status, both, or neither.
         * We handle all 4 combinations here rather than building a dynamic query,
         * which keeps the code readable for a project of this size.
         */
        if (utilityType != null && status != null) {
            // Filter by both type and status
            rates = utilityRateRepository.findByUtilityTypeAndStatus(utilityType, status);
        } else if (utilityType != null) {
            // Filter by type only — return all statuses for this type
            rates = utilityRateRepository.findByUtilityType(utilityType);
        } else if (status != null) {
            // Filter by status only
            rates = utilityRateRepository.findByStatus(status);
        } else {
            // No filters — return everything
            rates = utilityRateRepository.findAll();
        }

        return utilityRateMapper.toResponseList(rates);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-003 — GET /api/v1/utility-rates/{utilityRateId}
    // Returns a single utility rate by its UUID.
    // ─────────────────────────────────────────────────────────────────

    @Override
    public UtilityRateResponse getRateById(UUID utilityRateId) {
        log.debug("Fetching utility rate by id: {}", utilityRateId);

        UtilityRate rate = findRateOrThrow(utilityRateId);
        return utilityRateMapper.toResponse(rate);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-004 — PUT /api/v1/utility-rates/{utilityRateId}
    // Updates an existing utility rate's fields.
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UtilityRateResponse updateRate(UUID utilityRateId, UtilityRateRequest request) {
        log.info("Updating utility rate: id={}", utilityRateId);

        UtilityRate rate = findRateOrThrow(utilityRateId);

        /*
         * IMPORTANT FROM API REFERENCE v2.1 (Section 6.1):
         * "Updating a rate does not affect already-recorded charges."
         *
         * This service does NOT need to do anything about existing UtilityCharge
         * records here. They already have ratePerUnitSnapshot saved on them.
         * Changing this rate only affects future charge recordings.
         *
         * We also do NOT block updates based on whether this rate is ACTIVE or INACTIVE.
         * The Finance Officer might want to correct a typo on an INACTIVE rate for audit
         * accuracy. Both are allowed.
         */

        /*
         * If the utilityType is being changed (e.g. from WATER to ELECTRICITY),
         * we must check that the target type doesn't already have an ACTIVE rate.
         * This prevents creating an ambiguous active rate situation via an update.
         */
        if (!rate.getUtilityType().equals(request.getUtilityType())) {
            boolean targetTypeHasActiveRate = utilityRateRepository
                    .findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(
                            request.getUtilityType(), RateStatus.ACTIVE)
                    .isPresent();

            if (targetTypeHasActiveRate) {
                throw new DuplicateResourceException(
                        "Cannot change type to " + request.getUtilityType() +
                                " because an ACTIVE rate already exists for that type."
                );
            }
        }

        // Apply updates
        rate.setUtilityType(request.getUtilityType());
        rate.setRatePerUnit(request.getRatePerUnit());
        rate.setUnitDescription(request.getUnitDescription());
        rate.setEffectiveFrom(request.getEffectiveFrom());
        // NOTE: status is NOT changed by PUT. Status changes use PATCH /status (UTIL-005).

        UtilityRate updated = utilityRateRepository.save(rate);
        log.info("Utility rate updated: id={}", updated.getId());

        return utilityRateMapper.toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-005 — PATCH /api/v1/utility-rates/{utilityRateId}/status
    // Activates or deactivates a utility rate.
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UtilityRateResponse updateRateStatus(UUID utilityRateId, UtilityRateStatusRequest request) {
        log.info("Updating utility rate status: id={}, newStatus={}", utilityRateId, request.getStatus());

        UtilityRate rate = findRateOrThrow(utilityRateId);

        /*
         * BUSINESS RULE: If activating this rate, check that no other ACTIVE rate
         * exists for the same utility type.
         *
         * Example: You have WATER rate A (INACTIVE) and WATER rate B (ACTIVE).
         * If you try to activate A, the system must block it — two ACTIVE WATER
         * rates would make charge calculation ambiguous.
         */
        if (request.getStatus() == RateStatus.ACTIVE) {
            utilityRateRepository
                    .findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(
                            rate.getUtilityType(), RateStatus.ACTIVE)
                    .ifPresent(existingActive -> {
                        // If the found active rate is NOT the same one we're trying to activate,
                        // then we have a conflict.
                        if (!existingActive.getId().equals(utilityRateId)) {
                            throw new DuplicateResourceException(
                                    "Cannot activate this rate. Another ACTIVE rate already exists for " +
                                            rate.getUtilityType() + " (id: " + existingActive.getId() + ")." +
                                            " Deactivate it first."
                            );
                        }
                    });
        }

        /*
         * FROM API REFERENCE v2.1 (Section 6.1 Note):
         * "Deactivated rates are retained for audit history."
         * We NEVER delete a rate — we only change its status.
         */
        rate.setStatus(request.getStatus());
        UtilityRate updated = utilityRateRepository.save(rate);
        log.info("Utility rate status changed: id={}, status={}", updated.getId(), updated.getStatus());

        return utilityRateMapper.toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fetches a UtilityRate by ID or throws ResourceNotFoundException.
     * Used by getRateById, updateRate, and updateRateStatus to avoid
     * repeating the same find-or-throw pattern in every method.
     */
    private UtilityRate findRateOrThrow(UUID utilityRateId) {
        return utilityRateRepository.findById(utilityRateId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility Rate", utilityRateId));
    }
}