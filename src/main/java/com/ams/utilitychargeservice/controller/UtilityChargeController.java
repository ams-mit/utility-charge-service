package com.ams.utilitychargeservice.controller;

import com.ams.utilitychargeservice.dto.request.UtilityChargeRequest;
import com.ams.utilitychargeservice.dto.request.UtilityChargeUpdateRequest;
import com.ams.utilitychargeservice.dto.response.ApiResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeSummaryResponse;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.service.UtilityChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utility-charges")
@RequiredArgsConstructor
public class UtilityChargeController {

    private final UtilityChargeService utilityChargeService;

    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityChargeResponse>> recordCharge(@Valid @RequestBody UtilityChargeRequest request) {
        UtilityChargeResponse response = utilityChargeService.recordCharge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utility charge recorded successfully", response, null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<UtilityChargeResponse>>> getAllCharges(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) UtilityType utilityType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Pageable pageable) {
        Page<UtilityChargeResponse> response = utilityChargeService.getAllCharges(unitId, utilityType, year, month, pageable);
        return ResponseEntity.ok(ApiResponse.success("Utility charges retrieved successfully", response, null));
    }

    @GetMapping("/{utilityChargeId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<UtilityChargeResponse>> getChargeById(@PathVariable UUID utilityChargeId) {
        UtilityChargeResponse response = utilityChargeService.getChargeById(utilityChargeId);
        return ResponseEntity.ok(ApiResponse.success("Utility charge retrieved successfully", response, null));
    }

    @PutMapping("/{utilityChargeId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityChargeResponse>> updateCharge(
            @PathVariable UUID utilityChargeId,
            @Valid @RequestBody UtilityChargeUpdateRequest request) {
        UtilityChargeResponse response = utilityChargeService.updateCharge(utilityChargeId, request);
        return ResponseEntity.ok(ApiResponse.success("Utility charge updated successfully", response, null));
    }

    @DeleteMapping("/{utilityChargeId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Void>> deleteCharge(@PathVariable UUID utilityChargeId) {
        utilityChargeService.deleteCharge(utilityChargeId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("Utility charge deleted successfully", null, null));
    }

    @GetMapping("/units/{unitId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UtilityChargeResponse>>> getChargesForUnit(@PathVariable UUID unitId) {
        List<UtilityChargeResponse> response = utilityChargeService.getChargesForUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Utility charges for unit retrieved successfully", response, null));
    }

    @GetMapping("/units/{unitId}/period/{year}/{month}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UtilityChargeResponse>>> getChargesForUnitAndPeriod(
            @PathVariable UUID unitId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        List<UtilityChargeResponse> response = utilityChargeService.getChargesForUnitAndPeriod(unitId, year, month);
        return ResponseEntity.ok(ApiResponse.success("Utility charges for unit and period retrieved successfully", response, null));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<UtilityChargeSummaryResponse>> getSummary(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        UtilityChargeSummaryResponse response = utilityChargeService.getSummary(year, month);
        return ResponseEntity.ok(ApiResponse.success("Utility charge summary retrieved successfully", response, null));
    }
}
