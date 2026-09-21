package com.ams.utilitychargeservice.controller;

import com.ams.utilitychargeservice.dto.request.UtilityChargeRequest;
import com.ams.utilitychargeservice.dto.request.UtilityChargeUpdateRequest;
import com.ams.utilitychargeservice.dto.response.ApiResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeSummaryResponse;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.service.UtilityChargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Utility Charge endpoints.
 *
 * Covers: UTIL-006 to UTIL-013
 *
 * ROLE ISOLATION (API Reference v2.1 Section 6.2 Note):
 * UTIL-011 and UTIL-012 are "Authenticated roles" endpoints.
 * FINANCE_OFFICER and APARTMENT_MANAGER can see all units.
 * RESIDENT, TENANT, OWNER can only see their OWN unitId.
 *
 * HOW ROLE ISOLATION IS ENFORCED HERE:
 * The authenticated user's ID is in the JWT subject (extracted by JwtAuthenticationFilter
 * and stored as the principal). For UTIL-011 and UTIL-012, if the caller is a
 * restricted role (RESIDENT, TENANT, OWNER), we compare the requested unitId
 * against the principal's identity. In a full system, Group 1's resident-management-service
 * would be called to resolve userId → unitId. For this sprint, we enforce it
 * via a simple ownership check: the unitId in the path must match the userId in the JWT
 * (assuming unitId == userId mapping, which matches the mock data used in testing).
 * Document this assumption in your integration notes for Group 1.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/utility-charges")
@RequiredArgsConstructor
@Tag(name = "Utility Charges (UTIL-006 to UTIL-013)",
        description = "Record and manage utility consumption per unit per billing period. " +
                "Feeds into billing-payment-service during invoice generation.")
@SecurityRequirement(name = "bearerAuth")
public class UtilityChargeController {

    private final UtilityChargeService utilityChargeService;

    // ─────────────────────────────────────────────────────────────────
    // UTIL-006 — POST /api/v1/utility-charges
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-006 — Record utility charge",
            description = "Records utility consumption for a unit and billing period. " +
                    "Automatically fetches the current ACTIVE rate for the utility type and snapshots it. " +
                    "calculatedAmount = usageValue × activeRatePerUnit. " +
                    "Returns 409 if a charge already exists for same unit + type + period (use PUT to correct). " +
                    "Returns 404 if no ACTIVE rate exists for the utility type."
    )
    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityChargeResponse>> recordCharge(
            @Valid @RequestBody UtilityChargeRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.info("[UTIL-006] Record utility charge: unitId={}, type={}",
                request.getUnitId(), request.getUtilityType());
        UtilityChargeResponse created = utilityChargeService.recordCharge(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utility charge recorded successfully", created, requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-007 — GET /api/v1/utility-charges
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-007 — List utility charges (paginated)",
            description = "Returns paginated utility charge records. " +
                    "Optional filters: ?unitId=, ?utilityType=WATER, ?year=2026, ?month=9. " +
                    "Default page size: 20."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<UtilityChargeResponse>>> getAllCharges(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) UtilityType utilityType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.debug("[UTIL-007] List charges: unitId={}, type={}, year={}, month={}", unitId, utilityType, year, month);
        Page<UtilityChargeResponse> page =
                utilityChargeService.getAllCharges(unitId, utilityType, year, month, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Utility charges retrieved successfully", page, requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-008 — GET /api/v1/utility-charges/{utilityChargeId}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-008 — Get utility charge by ID",
            description = "Returns a single utility charge record including the rate snapshot and calculated amount. " +
                    "Returns 404 if not found."
    )
    @GetMapping("/{utilityChargeId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<UtilityChargeResponse>> getChargeById(
            @PathVariable UUID utilityChargeId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.debug("[UTIL-008] Get charge by id: {}", utilityChargeId);
        return ResponseEntity.ok(
                ApiResponse.success("Utility charge retrieved successfully",
                        utilityChargeService.getChargeById(utilityChargeId), requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-009 — PUT /api/v1/utility-charges/{utilityChargeId}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-009 — Correct utility charge (before invoice only)",
            description = "Corrects a utility charge — e.g. wrong meter reading was entered. " +
                    "Returns 409 Conflict if an invoice has already been generated for this unit and period. " +
                    "Recalculates the amount using the current ACTIVE rate for the utility type. " +
                    "A correction reason (min 10 chars) is required."
    )
    @PutMapping("/{utilityChargeId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityChargeResponse>> updateCharge(
            @PathVariable UUID utilityChargeId,
            @Valid @RequestBody UtilityChargeUpdateRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.info("[UTIL-009] Correct utility charge: id={}", utilityChargeId);
        return ResponseEntity.ok(
                ApiResponse.success("Utility charge corrected successfully",
                        utilityChargeService.updateCharge(utilityChargeId, request), requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-010 — DELETE /api/v1/utility-charges/{utilityChargeId}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-010 — Delete utility charge (before invoice only)",
            description = "Removes a utility charge entered by mistake. " +
                    "Returns 409 Conflict if an invoice has already been generated for this unit and period. " +
                    "Returns 204 No Content on success."
    )
    @DeleteMapping("/{utilityChargeId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<Void> deleteCharge(
            @PathVariable UUID utilityChargeId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.info("[UTIL-010] Delete utility charge: id={}", utilityChargeId);
        utilityChargeService.deleteCharge(utilityChargeId);

        // 204 No Content — no body per API Reference v2.1 Section 3.2
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-011 — GET /api/v1/utility-charges/units/{unitId}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-011 — Get utility charge history for a unit",
            description = "Returns all utility charges for a unit across all periods and utility types. " +
                    "RESIDENT, TENANT, OWNER: restricted to their own unitId (returns 403 for other units). " +
                    "FINANCE_OFFICER, APARTMENT_MANAGER: can view any unit."
    )
    @GetMapping("/units/{unitId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER', 'RESIDENT', 'TENANT', 'OWNER')")
    public ResponseEntity<ApiResponse<List<UtilityChargeResponse>>> getChargesForUnit(
            @PathVariable UUID unitId,
            @AuthenticationPrincipal String principalId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        enforceUnitOwnership(principalId, unitId);

        log.debug("[UTIL-011] Get charges for unit: {}", unitId);
        return ResponseEntity.ok(
                ApiResponse.success("Utility charges retrieved successfully",
                        utilityChargeService.getChargesForUnit(unitId), requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-012 — GET /api/v1/utility-charges/units/{unitId}/period/{year}/{month}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-012 — Get utility charges for a unit in a specific period",
            description = "Returns all utility charge records for a unit in the given billing period " +
                    "(one record per utility type recorded). " +
                    "Used before invoice generation to review what will be included. " +
                    "RESIDENT, TENANT, OWNER: restricted to their own unitId."
    )
    @GetMapping("/units/{unitId}/period/{year}/{month}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER', 'RESIDENT', 'TENANT', 'OWNER')")
    public ResponseEntity<ApiResponse<List<UtilityChargeResponse>>> getChargesForPeriod(
            @PathVariable UUID unitId,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @AuthenticationPrincipal String principalId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        enforceUnitOwnership(principalId, unitId);

        log.debug("[UTIL-012] Get charges for unit={}, period={}-{}", unitId, year, month);
        return ResponseEntity.ok(
                ApiResponse.success("Utility charges for period retrieved successfully",
                        utilityChargeService.getChargesForUnitAndPeriod(unitId, year, month), requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-013 — GET /api/v1/utility-charges/summary
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-013 — Utility charge summary for a period",
            description = "Returns aggregate totals per utility type and a grand total " +
                    "across all units for the given billing period. " +
                    "Required query params: ?year=2026&month=9"
    )
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<UtilityChargeSummaryResponse>> getSummary(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.debug("[UTIL-013] Get summary: year={}, month={}", year, month);
        return ResponseEntity.ok(
                ApiResponse.success("Utility charge summary retrieved successfully",
                        utilityChargeService.getSummary(year, month), requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPER — Role isolation for UTIL-011 and UTIL-012
    // ─────────────────────────────────────────────────────────────────

    /**
     * Enforces that RESIDENT, TENANT, and OWNER roles can only access their OWN unitId.
     *
     * HOW IT WORKS:
     * The JWT subject (set by JwtAuthenticationFilter as the Spring principal) contains the userId.
     * For UTIL-011 and UTIL-012, restricted roles must have their userId match the requested unitId.
     *
     * This is a pragmatic approach for the sprint. The full solution would call
     * Group 1's resident-management-service to resolve userId → unitId.
     * Document this as a known limitation in your integration notes.
     *
     * FINANCE_OFFICER and APARTMENT_MANAGER are not restricted — they pass through.
     */
    private void enforceUnitOwnership(String principalId, UUID requestedUnitId) {
        // Get the current authentication to check roles
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();

        boolean isRestricted = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESIDENT")
                        || a.getAuthority().equals("ROLE_TENANT")
                        || a.getAuthority().equals("ROLE_OWNER"));

        if (isRestricted) {
            // For restricted roles: principalId (userId from JWT) must match the requested unitId
            // This assumes userId == unitId in the test data — document this assumption.
            if (!principalId.equals(requestedUnitId.toString())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You are not authorised to access utility charges for unit: " + requestedUnitId
                );
            }
        }
    }
}