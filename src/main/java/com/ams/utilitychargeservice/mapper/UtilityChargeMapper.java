package com.ams.utilitychargeservice.mapper;

import com.ams.utilitychargeservice.dto.response.InternalUtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeResponse;
import com.ams.utilitychargeservice.entity.UtilityCharge;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts UtilityCharge JPA entities to UtilityChargeResponse DTOs
 * and InternalUtilityChargeResponse DTOs (used by Part 3 internal endpoint).
 *
 * Two different response shapes:
 * - UtilityChargeResponse        → full detail, returned to Finance Officer / Manager / Resident
 * - InternalUtilityChargeResponse → lightweight projection for billing-payment-service (UTIL-014)
 */
@Component
public class UtilityChargeMapper {

    public UtilityChargeResponse toResponse(UtilityCharge charge) {
        return UtilityChargeResponse.builder()
                .id(charge.getId())
                .unitId(charge.getUnitId())
                .utilityType(charge.getUtilityType())
                .billingYear(charge.getBillingYear())
                .billingMonth(charge.getBillingMonth())
                .usageValue(charge.getUsageValue())
                .ratePerUnitSnapshot(charge.getRatePerUnitSnapshot())
                .calculatedAmount(charge.getCalculatedAmount())
                .utilityRateId(charge.getUtilityRateId())
                .createdAt(charge.getCreatedAt())
                .createdBy(charge.getCreatedBy())
                .build();
    }

    public List<UtilityChargeResponse> toResponseList(List<UtilityCharge> charges) {
        return charges.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lightweight projection for UTIL-014 internal endpoint.
     * Only the fields billing-payment-service needs to create an invoice line item.
     */
    public InternalUtilityChargeResponse toInternalResponse(UtilityCharge charge) {
        return InternalUtilityChargeResponse.builder()
                .utilityChargeId(charge.getId())
                .utilityType(charge.getUtilityType())
                .usageValue(charge.getUsageValue())
                .ratePerUnitSnapshot(charge.getRatePerUnitSnapshot())
                .calculatedAmount(charge.getCalculatedAmount())
                .build();
    }

    public List<InternalUtilityChargeResponse> toInternalResponseList(List<UtilityCharge> charges) {
        return charges.stream()
                .map(this::toInternalResponse)
                .toList();
    }
}