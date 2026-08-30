package com.ams.utilitychargeservice.dto.response;

import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class UtilityRateResponse {

    private UUID id;
    private UtilityType utilityType;
    private BigDecimal ratePerUnit;
    private String unitDescription;
    private LocalDate effectiveFrom;
    private RateStatus status;
    private Instant createdAt;
    private String createdBy;
}