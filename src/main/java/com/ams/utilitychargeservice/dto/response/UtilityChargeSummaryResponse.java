package com.ams.utilitychargeservice.dto.response;

import com.ams.utilitychargeservice.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class UtilityChargeSummaryResponse {

    private Integer billingYear;
    private Integer billingMonth;
    private Map<UtilityType, BigDecimal> totalsByType;
    private BigDecimal grandTotal;
    private Long recordCount;
}