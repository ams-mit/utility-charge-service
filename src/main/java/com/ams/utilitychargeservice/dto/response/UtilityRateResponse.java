package com.ams.utilitychargeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityRateResponse {
    private UUID id;
    private String utilityType;
    private BigDecimal ratePerUnit;
    private String unitDescription;
    private LocalDate effectiveFrom;
    private String status;
    private Instant createdAt;
    private String createdBy;
}
