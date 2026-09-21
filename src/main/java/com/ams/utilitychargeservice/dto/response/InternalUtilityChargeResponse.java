package com.ams.utilitychargeservice.dto.response;

import com.ams.utilitychargeservice.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight projection used by billing-payment-service during invoice generation.
 * Only exposes what billing needs to create a line item.
 */
@Getter
@Builder
public class InternalUtilityChargeResponse {

    private UUID utilityChargeId;
    private UtilityType utilityType;
    private BigDecimal usageValue;
    private BigDecimal ratePerUnitSnapshot;
    private BigDecimal calculatedAmount;
}