package com.ams.utilitychargeservice.controller;

import com.ams.utilitychargeservice.dto.request.UtilityRateRequest;
import com.ams.utilitychargeservice.dto.request.UtilityRateStatusRequest;
import com.ams.utilitychargeservice.dto.response.ApiResponse;
import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.service.UtilityRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/utility-rates")
@RequiredArgsConstructor
public class UtilityRateController {

    private final UtilityRateService utilityRateService;

    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> createRate(@Valid @RequestBody UtilityRateRequest request) {
        UtilityRateResponse response = utilityRateService.createRate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utility rate created successfully", response, null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<UtilityRateResponse>>> getAllRates(
            @RequestParam(required = false) UtilityType utilityType,
            @RequestParam(required = false) RateStatus status) {
        List<UtilityRateResponse> response = utilityRateService.getAllRates(utilityType, status);
        return ResponseEntity.ok(ApiResponse.success("Utility rates retrieved successfully", response, null));
    }

    @GetMapping("/{utilityRateId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> getRateById(@PathVariable UUID utilityRateId) {
        UtilityRateResponse response = utilityRateService.getRateById(utilityRateId);
        return ResponseEntity.ok(ApiResponse.success("Utility rate retrieved successfully", response, null));
    }

    @PutMapping("/{utilityRateId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> updateRate(
            @PathVariable UUID utilityRateId,
            @Valid @RequestBody UtilityRateRequest request) {
        UtilityRateResponse response = utilityRateService.updateRate(utilityRateId, request);
        return ResponseEntity.ok(ApiResponse.success("Utility rate updated successfully", response, null));
    }

    @PatchMapping("/{utilityRateId}/status")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<UtilityRateResponse>> updateRateStatus(
            @PathVariable UUID utilityRateId,
            @Valid @RequestBody UtilityRateStatusRequest request) {
        UtilityRateResponse response = utilityRateService.updateRateStatus(utilityRateId, request);
        return ResponseEntity.ok(ApiResponse.success("Utility rate status updated successfully", response, null));
    }
}
