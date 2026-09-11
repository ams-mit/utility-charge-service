package com.ams.utilitychargeservice.controller.internal;

import com.ams.utilitychargeservice.dto.response.ApiResponse;
import com.ams.utilitychargeservice.dto.response.InternalUtilityChargeResponse;
import com.ams.utilitychargeservice.service.UtilityChargeService;
import com.ams.utilitychargeservice.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/utility-charges")
@RequiredArgsConstructor
@Slf4j
public class InternalUtilityController {

    private final UtilityChargeService utilityChargeService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/units/{unitId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<ApiResponse<List<InternalUtilityChargeResponse>>> getChargesForInvoiceGeneration(
            @PathVariable UUID unitId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            HttpServletRequest request) {

        // Security Check: Verify calling service identity (sub)
        // As per spec Section 2.7 and 6.3, this endpoint is provided to billing-payment-service
        String token = extractToken(request);
        String callingService = jwtTokenProvider.extractUserId(token);

        if (!"billing-payment-service".equals(callingService)) {
            log.warn("Unauthorized internal call attempt from service: {}", callingService);
            throw new AccessDeniedException("Calling service is not authorized to access this endpoint");
        }

        List<InternalUtilityChargeResponse> response = utilityChargeService.getChargesForInvoiceGeneration(unitId, year, month);
        return ResponseEntity.ok(ApiResponse.success("Internal utility charges retrieved successfully", response, null));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
