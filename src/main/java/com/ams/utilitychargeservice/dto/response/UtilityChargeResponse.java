package com.ams.utilitychargeservice.dto.response;

import com.ams.utilitychargeservice.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UtilityChargeResponse {

    private UUID id;
    private UUID unitId;
    private UtilityType utilityType;
    private Integer billingYear;
    private Integer billingMonth;
    private BigDecimal usageValue;
    private BigDecimal ratePerUnitSnapshot;
    private BigDecimal calculatedAmount;
    private UUID utilityRateId;
    private Instant createdAt;
    private String createdBy;
}