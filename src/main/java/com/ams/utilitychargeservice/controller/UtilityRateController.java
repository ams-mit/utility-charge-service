package com.ams.utilitychargeservice.controller;

import com.ams.utilitychargeservice.dto.request.UtilityRateRequest;
import com.ams.utilitychargeservice.dto.request.UtilityRateStatusRequest;
import com.ams.utilitychargeservice.dto.response.ApiResponse;
import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.service.UtilityRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Utility Rate endpoints.
 *
 * Covers: UTIL-001, UTIL-002, UTIL-003, UTIL-004, UTIL-005
 *
 * WHO CAN ACCESS:
 *   FINANCE_OFFICER   → all 5 endpoints
 *   APARTMENT_MANAGER → UTIL-002 and UTIL-003 (read-only access)
 *
 * AUTH FROM API REFERENCE v2.1:
 *   All requests arrive with a Gateway User JWT in Authorization: Bearer <token>
 *   The JwtAuthenticationFilter already validated the token and populated
 *   the SecurityContext with the user's roles before this controller is reached.
 *   @PreAuthorize annotations enforce the role rules per the API reference.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/utility-rates")
@RequiredArgsConstructor
@Tag(name = "Utility Rates (UTIL-001 to UTIL-005)",
        description = "Manage utility rate definitions — price per unit of consumption for water, electricity, gas, and parking")
@SecurityRequirement(name = "bearerAuth")
public class UtilityRateController {

    private final UtilityRateService utilityRateService;

    // ─────────────────────────────────────────────────────────────────
    // UTIL-001 — POST /api/v1/utility-rates
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-001 — Create utility rate",
            description = "Creates a new utility rate for a utility type. " +
                    "Returns 409 if an ACTIVE rate already exists for the same type. " +
                    "New rates are always created with ACTIVE status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Rate created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Active rate already exists for this utility type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Role not permitted")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> createRate(
            @Valid @RequestBody UtilityRateRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.info("[UTIL-001] Create utility rate request received: type={}", request.getUtilityType());
        UtilityRateResponse created = utilityRateService.createRate(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utility rate created successfully", created, requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-002 — GET /api/v1/utility-rates
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-002 — List utility rates",
            description = "Returns all utility rates. Optional query params: " +
                    "?utilityType=WATER (filter by type), ?status=ACTIVE (filter by status). " +
                    "Both filters can be combined."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rates retrieved")
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<UtilityRateResponse>>> getAllRates(
            @RequestParam(required = false) UtilityType utilityType,
            @RequestParam(required = false) RateStatus status,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.debug("[UTIL-002] Get all rates: utilityType={}, status={}", utilityType, status);
        List<UtilityRateResponse> rates = utilityRateService.getAllRates(utilityType, status);

        return ResponseEntity.ok(
                ApiResponse.success("Utility rates retrieved successfully", rates, requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-003 — GET /api/v1/utility-rates/{utilityRateId}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-003 — Get utility rate by ID",
            description = "Returns a single utility rate by its UUID. Returns 404 if not found."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rate retrieved")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rate not found")
    @GetMapping("/{utilityRateId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> getRateById(
            @PathVariable UUID utilityRateId,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.debug("[UTIL-003] Get rate by id: {}", utilityRateId);
        UtilityRateResponse rate = utilityRateService.getRateById(utilityRateId);

        return ResponseEntity.ok(
                ApiResponse.success("Utility rate retrieved successfully", rate, requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-004 — PUT /api/v1/utility-rates/{utilityRateId}
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-004 — Update utility rate",
            description = "Updates ratePerUnit, unitDescription, effectiveFrom, or utilityType. " +
                    "IMPORTANT: Updating a rate does NOT affect already-recorded utility charges. " +
                    "Those records have ratePerUnitSnapshot saved permanently at recording time. " +
                    "Status is NOT changed by this endpoint — use UTIL-005 for status changes."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rate updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rate not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Type change blocked — target type already has an active rate")
    @PutMapping("/{utilityRateId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> updateRate(
            @PathVariable UUID utilityRateId,
            @Valid @RequestBody UtilityRateRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.info("[UTIL-004] Update utility rate: id={}", utilityRateId);
        UtilityRateResponse updated = utilityRateService.updateRate(utilityRateId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Utility rate updated successfully", updated, requestId));
    }

    // ─────────────────────────────────────────────────────────────────
    // UTIL-005 — PATCH /api/v1/utility-rates/{utilityRateId}/status
    // ─────────────────────────────────────────────────────────────────

    @Operation(
            summary = "UTIL-005 — Change utility rate status",
            description = "Activates or deactivates a utility rate. " +
                    "Request body: { \"status\": \"ACTIVE\" } or { \"status\": \"INACTIVE\" }. " +
                    "Deactivated rates are retained for audit history — they are never deleted. " +
                    "Returns 409 if activating a rate when another ACTIVE rate already exists for the same type."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rate not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Another active rate already exists for this type")
    @PatchMapping("/{utilityRateId}/status")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> updateStatus(
            @PathVariable UUID utilityRateId,
            @Valid @RequestBody UtilityRateStatusRequest request,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        log.info("[UTIL-005] Update rate status: id={}, newStatus={}", utilityRateId, request.getStatus());
        UtilityRateResponse updated = utilityRateService.updateRateStatus(utilityRateId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Utility rate status updated successfully", updated, requestId));
    }
}